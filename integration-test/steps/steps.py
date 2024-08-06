import copy
import time
from urllib.parse import parse_qs, urlparse
from behave import *
import logging
from allure_commons._allure import attach
import behave as behave

import utility.session as session
import utility.utils as utils
import request_generator as requestgen
import constants as constants
import router as router



# ==============================================
# ============== System up step ================
# ==============================================

@given('a new session')
def clear_session(context):
  
    logging.debug("=======================================================")
    logging.debug("[Clear session] Start clearing previous data on session")
    session.clear_session(context)
    logging.debug("[Clear session] End clearing previous data on session")

# ==============================================

@given('systems up')
def system_up(context):
    
    responses = True

    if "systems up" not in context.precondition_cache:
        for key in context.config.userdata.get("services"):
            row = context.config.userdata.get("services").get(key)
            if row.get("healthcheck") is not None:
                url = row.get("url") + row.get("healthcheck")
                logging.debug(f"[Health check] calling: {key} -> {url}")
                subscription_key = row.get("subscription_key")
                headers = {'Content-Type': 'application/json'}
                if subscription_key is not None:
                    headers[constants.OCP_APIM_SUBSCRIPTION_KEY] = subscription_key
                status_code, _, _ = utils.execute_request(url, "get", headers, payload=None, type=constants.ResponseType.JSON)
                logging.debug(f"[Health check] Received response: {status_code}")
                responses &= (status_code == 200)

        if responses:
            context.precondition_cache.add("systems up")

    assert responses, f"health-check systems or subscription-key errors"

# ==============================================



# ==============================================
# =============== Generic steps ================
# ==============================================

@step('the execution of "{scenario_name}" was successful')
def step_impl(context, scenario_name):

    all_scenarios = [scenario for feature in context._runner.features for scenario in feature.walk_scenarios()]
    phase = ([scenario for scenario in all_scenarios if scenario_name in scenario.name] or [None])[0]
    text_step = ''.join([step.keyword + " " + step.name + "\n\"\"\"\n" + (step.text or '') + "\n\"\"\"\n" for step in phase.steps])
    context.execute_steps(text_step)

# ==============================================



# ==============================================
# =============== [GIVEN] steps ================
# ==============================================

@given('a waiting time of {time_in_seconds} second{notes}')
def wait_for_n_seconds(context, time_in_seconds, notes):
    logging.info(f"Waiting [{time_in_seconds}] second{notes}")
    time.sleep(int(time_in_seconds))
    logging.info(f"Wait time ended")

# ==============================================

@given('a single RPT with {number_of_transfers} transfers of which {number_of_stamps} are stamps')
def generate_single_rpt(context, number_of_transfers, number_of_stamps):

    if number_of_stamps == "none":
        number_of_stamps = "0"

    session_data = copy.deepcopy(context.config.userdata.get("test_data"))
    session_data = requestgen.create_payments(session_data, 1, int(number_of_transfers), number_of_mbd=int(number_of_stamps))

    # generate request
    request = requestgen.generate_nodoinviarpt(session_data)
    
    # update context with request and edit session data
    session.set_test_data(context, session_data)
    session.set_flow_data(context, constants.SESSION_DATA_REQ_BODY, request)

# ==============================================

@given('a valid checkPosition request')
def generate_checkposition(context):

    # generate request
    payment_notices = session.get_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES)
    request = requestgen.generate_checkposition(payment_notices)

    # update context with request and edit session data
    session.set_flow_data(context, constants.SESSION_DATA_REQ_BODY, request)

# ==============================================

@given('a valid activatePaymentNoticeV2 request on {index} RPT')
def generate_activatepaymentnotice(context, index):

    rpt_index = -1
    match index:
        case "first":
            rpt_index = 0
        case "second":
            rpt_index = 1
        case "third":
            rpt_index = 2
        case "fourth":
            rpt_index = 3
        case "fifth":
            rpt_index = 4

    # generate request
    test_data = session.get_test_data(context)
    payment_notices = session.get_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES)
    request = requestgen.generate_activatepaymentnotice(test_data, payment_notices, test_data['payments'][rpt_index])

    # update context with request and edit session data
    session.set_flow_data(context, constants.SESSION_DATA_REQ_BODY, request)

# ==============================================

@given('a valid closePaymentV2 request with outcome {outcome}')
def generate_activatepaymentnotice(context, outcome):
    
    # generate request
    test_data = session.get_test_data(context)
    payment_notices = session.get_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES)
    request = requestgen.generate_closepayment(test_data, payment_notices, outcome)

    # update context with request and edit session data
    session.set_flow_data(context, constants.SESSION_DATA_REQ_BODY, request)

# ==============================================

@given('a valid session identifier to be redirected to WISP dismantling')
def get_valid_sessionid(context):

    session_id = session.get_flow_data(context, constants.SESSION_DATA_SESSION_ID)
    split_session_id = session_id.split("_")
    utils.assert_show_message(len(split_session_id[0]) == 11, f"The session ID must contains the broker code as first part of the session identifier. Session ID: [{session_id}]")
    utils.assert_show_message(len(split_session_id[1]) == 36, f"The session ID must contains an UUID as second part of the session identifier. Session ID: [{session_id}]")

# ==============================================

@given('the {index} IUV code of the sent RPTs')
def get_iuv_from_session(context, index):

    rpt_index = -1
    match index:
        case "first":
            rpt_index = 0
        case "second":
            rpt_index = 1
        case "third":
            rpt_index = 2
        case "fourth":
            rpt_index = 3
        case "fifth":
            rpt_index = 4

    test_data = session.get_test_data(context)
    utils.assert_show_message('payments' in test_data and len(test_data['payments']) >= rpt_index + 1, f"No valid payments are defined in the session data.")

    payment = test_data['payments'][rpt_index]
    utils.assert_show_message('iuv' in payment, f"No valid IUV is defined for payment with index {rpt_index}.") 
    iuv = payment['iuv']

    iuvs = session.get_flow_data(context, constants.SESSION_DATA_IUVS)
    if iuvs is None:
        iuvs = [None, None, None, None, None]
    iuvs[rpt_index] = iuv
    session.set_flow_data(context, constants.SESSION_DATA_IUVS, iuvs)

# ==============================================



# ==============================================
# ================ [WHEN] steps ================
# ==============================================

@when('the {actor} sends a {primitive} action')
def send_primitive(context, actor, primitive):

    request = session.get_flow_data(context, constants.SESSION_DATA_REQ_BODY)
    url, subkey, content_type = router.get_primitive_url(context, primitive)

    headers = {}
    if content_type == constants.ResponseType.XML:
        headers = {'Content-Type': 'application/xml', 'SOAPAction': primitive, constants.OCP_APIM_SUBSCRIPTION_KEY: subkey}
    elif content_type == constants.ResponseType.JSON:
        headers = {'Content-Type': 'application/json', constants.OCP_APIM_SUBSCRIPTION_KEY: subkey}
    
    status_code, body_response, _ = utils.execute_request(url, "post", headers, request, content_type)
    session.set_flow_data(context, constants.SESSION_DATA_RES_CODE, status_code)
    session.set_flow_data(context, constants.SESSION_DATA_RES_BODY, body_response)
    session.set_flow_data(context, constants.SESSION_DATA_RES_CONTENTTYPE, content_type)

# ==============================================

@when('the user continue the session in WISP dismantling')
def send_sessionid_to_wispdismantling(context):

    url, _ = router.get_rest_url(context, "redirect")    
    headers = {'Content-Type': 'application/xml'}
    url += session.get_flow_data(context, constants.SESSION_DATA_SESSION_ID)
    status_code, _, response_headers = utils.execute_request(url, "get", headers, allow_redirect=False)
    location_header = response_headers['Location']
    attach(location_header, name="Received response")

    session.set_flow_data(context, constants.SESSION_DATA_RES_CODE, status_code)
    session.set_flow_data(context, constants.SESSION_DATA_RES_BODY, location_header)
    session.set_flow_data(context, constants.SESSION_DATA_RES_CONTENTTYPE, constants.ResponseType.XML)

# ==============================================

@when('the user searches for flow steps by IUVs')
def search_in_re_by_iuv(context):

    iuvs = session.get_flow_data(context, constants.SESSION_DATA_IUVS)
    creditor_institution = session.get_test_data(context)['creditor_institution']
    today = utils.get_current_date()

    re_events = []
    base_url, subkey = router.get_rest_url(context, "search_in_re_by_iuv")
    headers = {'Content-Type': 'application/json', constants.OCP_APIM_SUBSCRIPTION_KEY: subkey}

    for iuv in iuvs:
        if iuv is not None:
            url = base_url.format(
                creditor_institution=creditor_institution,
                iuv=iuv,
                date_from=today,
                date_to=today
            )
            status_code, body_response, _ = utils.execute_request(url, "get", headers, type=constants.ResponseType.JSON)
            utils.assert_show_message('data' in body_response, f"The response does not contains data.")
            utils.assert_show_message(len(body_response['data']) > 0, f"There are not event data in the response.")
            re_events.extend(body_response['data'])

    session.set_flow_data(context, constants.SESSION_DATA_RES_CODE, status_code)
    session.set_flow_data(context, constants.SESSION_DATA_RES_BODY, re_events)
    
# ==============================================



# ==============================================
# ================ [THEN] steps ================
# ==============================================

@then('the {actor} receives the HTTP status code {status_code}')
def check_status_code(context, actor, status_code):

    status_code = session.get_flow_data(context, constants.SESSION_DATA_RES_CODE)
    utils.assert_show_message(status_code == int(status_code), f"The status code is not 200. Current value: {status_code}.")

# ==============================================

@then('the response contains the field {field_name} with value {field_value}')
def check_field(context, field_name, field_value):

    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    content_type = session.get_flow_data(context, constants.SESSION_DATA_RES_CONTENTTYPE)
    if content_type == constants.ResponseType.XML:
        field_value_in_object = response.find(f'.//{field_name}')
        utils.assert_show_message(field_value_in_object is not None, f"The field [{field_name}] does not exists.")
        field_value_in_object = field_value_in_object.text
    elif content_type == constants.ResponseType.JSON:
        field_value_in_object = utils.get_nested_field(response, field_name)
        utils.assert_show_message(field_value_in_object is not None, f"The field [{field_name}] does not exists.")
        
    utils.assert_show_message(field_value_in_object == field_value, f"The field [{field_name}] is not equals to {field_value}. Current value: {field_value_in_object}.")
    
# ==============================================

@then('the response contains the field {field_name} as not empty list')
def check_field(context, field_name):

    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    content_type = session.get_flow_data(context, constants.SESSION_DATA_RES_CONTENTTYPE)
    if content_type == constants.ResponseType.XML:
        field_value_in_object = response.find(f'.//{field_name}')
    elif content_type == constants.ResponseType.JSON:
        field_value_in_object = utils.get_nested_field(response, field_name)
    
    utils.assert_show_message(field_value_in_object is not None, f"The field [{field_name}] does not exists.")
    utils.assert_show_message(len(field_value_in_object) > 0, f"The field [{field_name}] is empty but is required to be not empty.")

# ==============================================

@then('the response contains the field {field_name} with non-null value')
def check_field(context, field_name):

    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    content_type = session.get_flow_data(context, constants.SESSION_DATA_RES_CONTENTTYPE)
    if content_type == constants.ResponseType.XML:
        field_value_in_object = response.find(f'.//{field_name}')
    elif content_type == constants.ResponseType.JSON:
        field_value_in_object = utils.get_nested_field(response, field_name)
    utils.assert_show_message(field_value_in_object is not None, f"The field [{field_name}] does not exists.")
    

@then('there is a {business_process} event with field {field_name} with value {field_value}')
def check_field(context, business_process, field_name, field_value):

    re_events = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    needed_process_events = [re_event for re_event in re_events if 'businessProcess' in re_event and re_event['businessProcess'] == business_process]
    utils.assert_show_message(len(needed_process_events) > 0, f"There are not events with business process {business_process}.")

    needed_events = [re_event for re_event in needed_process_events if field_name in re_event and re_event[field_name] == field_value]
    utils.assert_show_message(len(needed_events) > 0, f"There are not events with business process {business_process} and field {field_name} with value [{field_value}].")
    
# ==============================================

@then('the response contains the redirect URL')
def check_redirect_url(context):

    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    redirect_url = response.find('.//url')
    utils.assert_show_message(redirect_url is not None, f"The field 'redirect_url' in response doesn't exists.")

    parsed_url = urlparse(redirect_url.text)
    query_params = parse_qs(parsed_url.query)
    id_session = query_params['idSession'][0] if len(query_params['idSession']) > 0 else None
    utils.assert_show_message(id_session is not None, f"The field 'idSession' in response is not correctly set.")

    session.set_flow_data(context, constants.SESSION_DATA_SESSION_ID, id_session)

# ==============================================

@then('the user can be redirected to Checkout')
def check_redirect_url(context):

    location_redirect_url = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    utils.assert_show_message(location_redirect_url is not None, f"The header 'Location' does not exists.")
    utils.assert_show_message("ecommerce/checkout" in location_redirect_url, f"The header 'Location' does not refers to Checkout service. {location_redirect_url}")
  
# ==============================================  

@then('the notice number can be retrieved')
def retrieve_payment_notice_from_re_event(context):

    re_events = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    needed_events = [re_event for re_event in re_events if 'status' in re_event and re_event['status'] == "SAVED_RPT_IN_CART_RECEIVED_REDIRECT_URL_FROM_CHECKOUT"]
    utils.assert_show_message(len(needed_events) > 0, f"The redirect process is not ended successfully or there are missing events in RE")

    notices = set([(re_event['domainId'], re_event['iuv'], re_event['noticeNumber']) for re_event in needed_events])
    utils.assert_show_message(len(notices) > 0, f"Impossible to extract payment notices from events in RE")
    utils.assert_show_message(len(notices) == len(needed_events), f"Impossible to extract unique payment notices from IUV codes")

    payment_notices = []
    for payment_notice in notices:
        payment_notices.append({
            "domain_id": payment_notice[0],
            "iuv": payment_notice[1],
            "notice_number": payment_notice[2],
            "payment_token": None
        })

    session.set_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES, payment_notices)

# ==============================================  

@then('the payment token can be retrieved and associated to {index} RPT')
def retrieve_payment_token_from_activatepaymentnotice(context, index):

    rpt_index = -1
    match index:
        case "first":
            rpt_index = 0
        case "second":
            rpt_index = 1
        case "third":
            rpt_index = 2
        case "fourth":
            rpt_index = 3
        case "fifth":
            rpt_index = 4

    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    field_value_in_object = response.find('.//paymentToken')
    
    payment_notices = session.get_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES)
    utils.assert_show_message(len(payment_notices) >= rpt_index + 1, f"Not enough payment notices are defined in the session data for correctly point at index {rpt_index}.")

    payment_notice = payment_notices[rpt_index]
    utils.assert_show_message('iuv' in payment_notice, f"No valid payment is defined at index {rpt_index}.") 
    payment_notice['payment_token'] = field_value_in_object.text
    session.set_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES, payment_notices)