import base64
import copy
import re
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
def generate_session(context):
  
    logging.debug("=======================================================")
    logging.debug("[Generate session] Start re-generating data on session")
    session.clear_session(context)

    # update context with request and edit session data
    session_data = copy.deepcopy(context.config.userdata.get("test_data"))
    session.set_test_data(context, session_data)
    session.set_flow_data(context, constants.SESSION_DATA_RAW_RPTS, [])
    session.set_flow_data(context, constants.SESSION_DATA_TRIGGER_PRIMITIVE, constants.PRIMITIVE_NODOINVIARPT) # default value

    logging.debug("[Clear session] End re-generating data on session")

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

    session.set_skip_tests(context, False)
    logging.info(f"Waiting [{time_in_seconds}] second{notes}")
    time.sleep(int(time_in_seconds))
    logging.info(f"Wait time ended")

# ==============================================

@given('a cart of RPTs {note}')
def generate_empty_cart(context, note):

    test_data = session.get_test_data(context)
    session.set_flow_data(context, constants.SESSION_DATA_TRIGGER_PRIMITIVE, constants.PRIMITIVE_NODOINVIACARRELLORPT)
    
    if "for multibeneficiary" in note:
        iuv = utils.generate_iuv()
        session.set_flow_data(context, constants.SESSION_DATA_CART_ID, utils.generate_cart_id(iuv, test_data['creditor_institution']))
        session.set_flow_data(context, constants.SESSION_DATA_CART_IS_MULTIBENEFICIARY, True)
        session.set_flow_data(context, constants.SESSION_DATA_CART_MULTIBENEFICIARY_IUV, iuv)
    else:
        session.set_flow_data(context, constants.SESSION_DATA_CART_ID, utils.generate_cart_id(None, test_data['creditor_institution']))
        session.set_flow_data(context, constants.SESSION_DATA_CART_IS_MULTIBENEFICIARY, False)

# ==============================================

@given('a single RPT of type {payment_type} with {number_of_transfers} transfers of which {number_of_stamps} are stamps')
def generate_single_rpt(context, payment_type, number_of_transfers, number_of_stamps):

    session.set_skip_tests(context, False)

    if number_of_stamps == "none":
        number_of_stamps = "0"
    
    # force IUV definition if the RPT is part of multibeneficiary cart
    iuv = None
    is_multibeneficiary_cart = session.get_flow_data(context, constants.SESSION_DATA_CART_IS_MULTIBENEFICIARY)
    if is_multibeneficiary_cart is not None and is_multibeneficiary_cart == True:
        iuv = session.get_flow_data(context, constants.SESSION_DATA_CART_MULTIBENEFICIARY_IUV)

    
    # force CCP definition if the RPT is part of multibeneficiary cart
    ccp = None
    if is_multibeneficiary_cart:
        ccp = session.get_flow_data(context, constants.SESSION_DATA_CART_ID)

    # generate raw RPT that will be used for construct XML content
    test_data = session.get_test_data(context)
    domain_id = test_data['creditor_institution'] 
    payee_institution = test_data['payee_institutions_1']
    rpt = requestgen.create_rpt(test_data, iuv, ccp, domain_id, payee_institution, payment_type, int(number_of_transfers), int(number_of_stamps))
    
    # update list of RPTs
    rpts = session.get_flow_data(context, constants.SESSION_DATA_RAW_RPTS)
    rpts.append(rpt)
    session.set_flow_data(context, constants.SESSION_DATA_RAW_RPTS, rpts)

# ==============================================

@given('an existing payment position related to {index} RPT with segregation code equals to {segregation_code} and state equals to {payment_status}')
def generate_payment_position(context, index, segregation_code, payment_status):

    session.set_skip_tests(context, False)

    payment_notice_index = utils.get_index_from_cardinal(index)

    raw_rpts = session.get_flow_data(context, constants.SESSION_DATA_RAW_RPTS)
    rpt = raw_rpts[payment_notice_index]

    #primitive = session.get_flow_data(context, constants.SESSION_DATA_TRIGGER_PRIMITIVE)
    #elif primitive == constants.PRIMITIVE_NODOINVIACARRELLORPT:
    #    TODO to update

    payment_positions = requestgen.generate_gpd_paymentposition(context, rpt, segregation_code, payment_status)

    if payment_status == "VALID":
        base_url, subkey = router.get_rest_url(context, "create_paymentposition_and_publish")
    else:
        base_url, subkey = router.get_rest_url(context, "create_paymentposition")
    url = base_url.format(creditor_institution=rpt['domain']['id'])

    headers = {'Content-Type': 'application/json', constants.OCP_APIM_SUBSCRIPTION_KEY: subkey}
    status_code, _, _ = utils.execute_request(url, "post", headers, payment_positions, type=constants.ResponseType.JSON)

    utils.assert_show_message(status_code == 201, f"The debt position for RPT with index [{index}] was not created. Expected status code [201], Current status code [{status_code}]")

# ==============================================

@given('a valid nodoInviaRPT request')
def generate_nodoinviarpt(context):
    
    session.set_skip_tests(context, False)

    # generate request
    test_data = session.get_test_data(context)
    raw_rpts = session.get_flow_data(context, constants.SESSION_DATA_RAW_RPTS)
    request = requestgen.generate_nodoinviarpt(test_data, raw_rpts[0])
    
    # update context with request and edit session data
    session.set_flow_data(context, constants.SESSION_DATA_REQ_BODY, request)

# ==============================================

@given('a valid nodoInviaCarrelloRPT request')
def generate_nodoinviarpt(context):
    
    session.set_skip_tests(context, False)

    # generate request
    test_data = session.get_test_data(context)
    rpts = session.get_flow_data(context, constants.SESSION_DATA_RAW_RPTS)
    cart_id = session.get_flow_data(context, constants.SESSION_DATA_CART_ID)
    request = requestgen.generate_nodoinviacarrellorpt(test_data, cart_id, rpts)
    
    # update context with request and edit session data
    session.set_flow_data(context, constants.SESSION_DATA_REQ_BODY, request)

# ==============================================

@given('a valid checkPosition request')
def generate_checkposition(context):

    session.set_skip_tests(context, False)

    # generate request
    payment_notices = session.get_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES)
    request = requestgen.generate_checkposition(payment_notices)

    # update context with request and edit session data
    session.set_flow_data(context, constants.SESSION_DATA_REQ_BODY, request)

# ==============================================

@given('a valid activatePaymentNoticeV2 request on {index} payment notice')
def generate_activatepaymentnotice(context, index):

    session.set_skip_tests(context, False)

    payment_notice_index = utils.get_index_from_cardinal(index)
    payment_notices = session.get_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES)

    # check if RPT exists
    # TODO to change when multibeneficiary will be handled
    if payment_notice_index + 1 > len(payment_notices):
        session.set_skip_tests(context, True)
        context.scenario.skip(f"Skipping scenario because the {index} RPT does not exist.")
        return

    # generate request
    test_data = session.get_test_data(context)
    session_id = session.get_flow_data(context, constants.SESSION_DATA_SESSION_ID)
    # TODO to change when multibeneficiary will be handled
    rpts = session.get_flow_data(context, constants.SESSION_DATA_RAW_RPTS)
    request = requestgen.generate_activatepaymentnotice(test_data, payment_notices, rpts[payment_notice_index], session_id) 

    # update context with request and edit session data
    session.set_flow_data(context, constants.SESSION_DATA_REQ_BODY, request)

# ==============================================

@given('a valid closePaymentV2 request with outcome {outcome}')
def generate_closepayment(context, outcome):
    
    session.set_skip_tests(context, False)

    # generate request
    test_data = session.get_test_data(context)
    payment_notices = session.get_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES)
    rpts = session.get_flow_data(context, constants.SESSION_DATA_RAW_RPTS)
    request = requestgen.generate_closepayment(test_data, payment_notices, rpts, outcome)

    # update context with request and edit session data
    session.set_flow_data(context, constants.SESSION_DATA_REQ_BODY, request)

# ==============================================

@given('a valid session identifier to be redirected to WISP dismantling')
def get_valid_sessionid(context):

    session.set_skip_tests(context, False)

    session_id = session.get_flow_data(context, constants.SESSION_DATA_SESSION_ID)
    utils.assert_show_message(len(session_id) == 36, f"The session ID must consist of a UUID only. Session ID: [{session_id}]")

# ==============================================

@given('the {index} IUV code of the sent RPTs')
def get_iuv_from_session(context, index):

    session.set_skip_tests(context, False)

    rpt_index = utils.get_index_from_cardinal(index)
    raw_rpts = session.get_flow_data(context, constants.SESSION_DATA_RAW_RPTS)
    
    # check if IUV exists
    # TODO to change when multibeneficiary will be handled
    if rpt_index + 1 > len(raw_rpts):
        session.set_skip_tests(context, True)
        context.scenario.skip(f"Skipping scenario because the {index} IUV does not exist.")
        return
    
    iuv = raw_rpts[rpt_index]['payment_data']['iuv']

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

    if session.skip_tests(context):
        logging.debug("Skipping send_primitive step")
        return

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
    status_code, response_body, response_headers = utils.execute_request(url, "get", headers, type=constants.ResponseType.HTML, allow_redirect=False)
    if 'Location' in response_headers:
        location_header = response_headers['Location']
        attach(location_header, name="Received response")
        session.set_flow_data(context, constants.SESSION_DATA_RES_BODY, location_header)
    else:
        attach(response_body, name="Received response")
        session.set_flow_data(context, constants.SESSION_DATA_RES_BODY, response_body)

    session.set_flow_data(context, constants.SESSION_DATA_RES_CODE, status_code)    
    session.set_flow_data(context, constants.SESSION_DATA_RES_CONTENTTYPE, constants.ResponseType.HTML)

# ==============================================

@when('the user searches for flow steps by IUVs')
def search_in_re_by_iuv(context):

    if session.skip_tests(context):
        logging.debug("Skipping search_in_re_by_iuv step")
        return

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

@when('the user searches for payment position in GPD by {index} IUV')
def search_paymentposition_by_iuv(context, index):

    rpt_index = utils.get_index_from_cardinal(index)
    payment_notices = session.get_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES)  

    if rpt_index + 1 > len(payment_notices):
        session.set_skip_tests(context, True)
        context.scenario.skip(f"Skipping scenario because the {index} IUV does not exist.")
        return
  
    payment_notice = payment_notices[rpt_index]
    base_url, subkey = router.get_rest_url(context, "get_paymentposition_by_iuv")
    url = base_url.format(creditor_institution=payment_notice['domain_id'], iuv=payment_notice['iuv'])
    headers = {'Content-Type': 'application/json', constants.OCP_APIM_SUBSCRIPTION_KEY: subkey}
    status_code, body_response, _ = utils.execute_request(url, "get", headers, type=constants.ResponseType.JSON)

    session.set_flow_data(context, constants.SESSION_DATA_RES_CODE, status_code)
    session.set_flow_data(context, constants.SESSION_DATA_RES_BODY, body_response)
    session.set_flow_data(context, constants.SESSION_DATA_RES_CONTENTTYPE, constants.ResponseType.JSON)
    
# ==============================================



# ==============================================
# ================ [THEN] steps ================
# ==============================================

@then('the {actor} receives the HTTP status code {status_code}')
def check_status_code(context, actor, status_code):

    if session.skip_tests(context):
        logging.debug("Skipping check_status_code step")
        return
    
    status_code = session.get_flow_data(context, constants.SESSION_DATA_RES_CODE)
    utils.assert_show_message(status_code == int(status_code), f"The status code is not 200. Current value: {status_code}.")

# ==============================================

@then('the {actor} receives an HTML page with an error')
def check_html_error_page(context, actor):

    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    utils.assert_show_message('<!DOCTYPE html>' in response, f"The response is not an HTML page")
    utils.assert_show_message('Si &egrave; verificato un errore imprevisto' in response, f"The HTML page does not contains an error message.")

# ==============================================

@then('the response contains the field {field_name} with value {field_value}')
def check_field(context, field_name, field_value):

    if session.skip_tests(context):
        logging.debug("Skipping check_field step")
        return
    
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

    if session.skip_tests(context):
        logging.debug("Skipping check_field step")
        return
    
    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    content_type = session.get_flow_data(context, constants.SESSION_DATA_RES_CONTENTTYPE)
    if content_type == constants.ResponseType.XML:
        field_value_in_object = response.find(f'.//{field_name}')
    elif content_type == constants.ResponseType.JSON:
        field_value_in_object = utils.get_nested_field(response, field_name)
    utils.assert_show_message(field_value_in_object is not None, f"The field [{field_name}] does not exists.")
    
# ==============================================

@then('there is a {business_process} event with field {field_name} with value {field_value}')
def check_event(context, business_process, field_name, field_value):

    if session.skip_tests(context):
        logging.debug("Skipping check_event step")
        return
    
    re_events = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    needed_process_events = [re_event for re_event in re_events if 'businessProcess' in re_event and re_event['businessProcess'] == business_process]
    utils.assert_show_message(len(needed_process_events) > 0, f"There are not events with business process {business_process}.")

    needed_events = [re_event for re_event in needed_process_events if field_name in re_event and re_event[field_name] == field_value]
    utils.assert_show_message(len(needed_events) > 0, f"There are not events with business process {business_process} and field {field_name} with value [{field_value}].")
    
    session.set_flow_data(context, constants.SESSION_DATA_LAST_ANALYZED_RE_EVENT, needed_events)

# ==============================================

@then('the response contains the {url_type} URL')
def check_redirect_url(context, url_type):

    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    url = response.find('.//url')
    utils.assert_show_message(url is not None, f"The field 'redirect_url' in response doesn't exists.")
    extracted_url = url.text

    parsed_url = urlparse(extracted_url)
    query_params = parse_qs(parsed_url.query)
    id_session = query_params['idSession'][0] if len(query_params['idSession']) > 0 else None
    utils.assert_show_message(id_session is not None, f"The field 'idSession' in response is not correctly set.")
    session.set_flow_data(context, constants.SESSION_DATA_SESSION_ID, id_session)

    if "redirect" in url_type:
        utils.assert_show_message("wisp-converter" in extracted_url, f"The URL is not the one defined for WISP dismantling.")
    elif "old WISP" in url_type:
        utils.assert_show_message("wallet" in extracted_url, f"The URL is not the one defined for old WISP.")

# ==============================================

@then('the user can be redirected to Checkout')
def check_redirect_url(context):

    location_redirect_url = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    utils.assert_show_message(location_redirect_url is not None, f"The header 'Location' does not exists.")
    utils.assert_show_message("ecommerce/checkout" in location_redirect_url, f"The header 'Location' does not refers to Checkout service. {location_redirect_url}")
  
# ==============================================  

@then('all the related notice numbers can be retrieved')
def retrieve_payment_notice_from_re_event(context):

    re_events = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    needed_events = [re_event for re_event in re_events if 'status' in re_event and re_event['status'] == "SAVED_RPT_IN_CART_RECEIVED_REDIRECT_URL_FROM_CHECKOUT"]
    utils.assert_show_message(len(needed_events) > 0, f"The redirect process is not ended successfully or there are missing events in RE")

    notices = set([(re_event['domainId'], re_event['iuv'], re_event['noticeNumber']) for re_event in needed_events])
    utils.assert_show_message(len(notices) > 0, f"Impossible to extract payment notices from events in RE")
    
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

    if session.skip_tests(context):
        logging.debug("Skipping retrieve_payment_token_from_activatepaymentnotice step")
        return
    
    rpt_index = utils.get_index_from_cardinal(index)

    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    field_value_in_object = response.find('.//paymentToken')
    
    payment_notices = session.get_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES)
    utils.assert_show_message(len(payment_notices) >= rpt_index + 1, f"Not enough payment notices are defined in the session data for correctly point at index {rpt_index}.")

    payment_notice = payment_notices[rpt_index]
    utils.assert_show_message('iuv' in payment_notice, f"No valid payment is defined at index {rpt_index}.") 
    payment_notice['payment_token'] = field_value_in_object.text
    session.set_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES, payment_notices)

# ==============================================  

@then('the response contains a single payment option')
def check_single_paymentoption(context):

    if session.skip_tests(context):
        logging.debug("Skipping check_single_paymentoption step")
        return
    
    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    utils.assert_show_message('paymentOption' in response, f"No field 'paymentOption' is defined for the retrieved payment position.") 
    payment_options = utils.get_nested_field(response, "paymentOption")
    utils.assert_show_message(len(payment_options) == 1, f"There is not only one payment option in the payment position. Found number {len(payment_options)}.") 

    # sorting transfer by transfer ID in order to avoid strange comparations
    transfers = payment_options[0]['transfer']
    transfers = sorted(transfers, key=lambda transfer: transfer['idTransfer'])
    payment_options[0]['transfer'] = transfers
    response['paymentOption'] = payment_options
    session.set_flow_data(context, constants.SESSION_DATA_RES_BODY, response)

# ==============================================  

@then('the response contains the payment option correctly generated from {index} RPT')
def check_paymentoption_amounts(context, index):

    if session.skip_tests(context):
        logging.debug("Skipping check_paymentoption_amounts step")
        return
    
    rpt_index = utils.get_index_from_cardinal(index)

    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    payment_options = utils.get_nested_field(response, "paymentOption")
    payment_option = payment_options[0]

    payment_notices = session.get_flow_data(context, constants.SESSION_DATA_PAYMENT_NOTICES)  
    payment_notice = payment_notices[rpt_index]

    rpts = session.get_flow_data(context, constants.SESSION_DATA_RAW_RPTS)
    rpt = [rpt for rpt in rpts if rpt['payment_data']['iuv'] == payment_notice['iuv']][0]
    payment_data = rpt['payment_data']

    utils.assert_show_message(response['pull'] == False, f"The payment option must be not defined for pull payments.")
    utils.assert_show_message(int(payment_option['amount']) == round(payment_data['total_amount'] * 100), f"The total amount calculated for {index} RPT is not equals to the one defined in GPD payment position. GPD's: [{int(payment_option['amount'])}], RPT's: [{round(payment_data['total_amount'])}]") 
    utils.assert_show_message(payment_option['notificationFee'] == 0, f"The notification fee in the {index} payment position defined for GPD must be always 0.") 
    utils.assert_show_message(payment_option['isPartialPayment'] == False, f"The payment option must be not defined as partial payment.") 

# ==============================================  

@then('the response contains the status in {status} for the payment option')
def check_paymentposition_status(context, status):

    if session.skip_tests(context):
        logging.debug("Skipping check_paymentposition_status step")
        return
    
    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    payment_options = utils.get_nested_field(response, "paymentOption")
    payment_option = payment_options[0]
    utils.assert_show_message(payment_option['status'] == status, f"The payment option must be equals to [{status}]. Current status: [{payment_option['status']}]")

# ==============================================  

@then('the response contains the transfers correctly generated from {index} RPT in nodoInviaRPT')
def check_paymentposition_transfers(context, index):

    if session.skip_tests(context):
        logging.debug("Skipping check_paymentposition_transfers step")
        return
    
    rpt_index = utils.get_index_from_cardinal(index)
    
    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    payment_options = utils.get_nested_field(response, "paymentOption")
    transfers_from_po = payment_options[0]['transfer']

    raw_rpts = session.get_flow_data(context, constants.SESSION_DATA_RAW_RPTS)
    rpt = [rpt for rpt in raw_rpts if rpt['payment_data']['iuv'] == payment_options[0]['iuv']][0]
    transfers_from_rpt = rpt['payment_data']['transfers']

    utils.assert_show_message(len(transfers_from_po) == len(transfers_from_rpt), f"There are not the same amount of transfers. GPD's: [{len(transfers_from_po)}], RPT's: [{len(transfers_from_rpt)}]")

    for transfer_index in range(len(transfers_from_po)):
        transfer_from_po = transfers_from_po[transfer_index]
        transfer_from_rpt = transfers_from_rpt[transfer_index]
        utils.assert_show_message(transfer_from_po['status'] == "T_UNREPORTED", f"The status of the transfer {transfer_index} must be equals to [T_UNREPORTED]. Current status: [{transfer_from_po['status']}]")
        utils.assert_show_message(int(transfer_from_po['amount']) == round(transfer_from_rpt['amount'] * 100), f"The amount of the transfer {transfer_index} must be equals to the same defined in the payment position. GPD's: [{int(transfer_from_po['amount'])}], RPT's: [{round(transfer_from_rpt['amount'])}]")
        utils.assert_show_message('transferMetadata' in transfer_from_po and len(transfer_from_po['transferMetadata']) > 0, f"There are not transfer metadata in transfer {transfer_index} but at least one is required.")
        utils.assert_show_message('stamp' in transfer_from_po or 'iban' in transfer_from_po, f"There are either IBAN and stamp definition in transfer {transfer_index} but they cannot be defined together.")
        if transfer_from_rpt['is_mbd'] == True:
            utils.assert_show_message('stamp' in transfer_from_po, f"There is not stamp definition in transfer {transfer_index} but RPT transfer define it.")
            utils.assert_show_message('hashDocument' in transfer_from_po['stamp'], f"There is not a valid hash for stamp in transfer {transfer_index}.")
            utils.assert_show_message('stampType' in transfer_from_po['stamp'], f"There is not a valid type for stamp in transfer {transfer_index}.")
            utils.assert_show_message(transfer_from_po['stamp']['hashDocument'] == transfer_from_rpt['stamp_hash'], f"The hash defined for the stamp in payment position in transfer {transfer_index} is not equals to the one defined in RPT. GPD's: [{transfer_from_po['stamp']['hashDocument']}], RPT's: [{transfer_from_rpt['stamp_hash']}]")
            utils.assert_show_message(transfer_from_po['stamp']['stampType'] == transfer_from_rpt['stamp_type'], f"The type defined for the stamp in payment position in transfer {transfer_index} is not equals to the one defined in RPT. GPD's: [{transfer_from_po['stamp']['stampType']}], RPT's: [{transfer_from_rpt['stamp_type']}]")
        else:
            utils.assert_show_message('iban' in transfer_from_po, f"There is not IBAN definition in transfer {transfer_index} but RPT transfer define it.")    
            utils.assert_show_message(transfer_from_po['iban'] == transfer_from_rpt['creditor_iban'], f"The IBAN defined in transfer {transfer_index} is not equals to the one defined in RPT. GPD's: [{transfer_from_po['iban']}], RPT's: [{transfer_from_rpt['creditor_iban']}]")
