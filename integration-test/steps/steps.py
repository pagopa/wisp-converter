import copy
import re
from urllib.parse import parse_qs, urlparse
from behave import *
from datetime import timezone, timedelta
import logging
import requests
from allure_commons._allure import attach
import behave as behave

import session as session
import utils as utils
import request_generator as requestgen
import constants as constants
import router as router



# ==============================================
# ============== System up step ================
# ==============================================

@given('a new session')
def clear_session(context):

    session.clear_session(context)

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
                resp = requests.get(url, headers=headers, verify=False)
                logging.debug(f"[Health check] Received response: {resp.status_code}")
                responses &= (resp.status_code == 200)

        if responses:
            context.precondition_cache.add("systems up")

    assert responses, f"health-check systems or subscription-key errors"

# ==============================================



# ==============================================
# =============== [GIVEN] steps ================
# ==============================================

@given('a single RPT with {number_of_transfers} transfers')
def generate_single_rpt(context, number_of_transfers):

    session_data = copy.deepcopy(context.config.userdata.get("test_data"))
    session_data = requestgen.create_payments(session_data, 1, int(number_of_transfers))

    # generate request
    request = requestgen.generate_nodoinviarpt(session_data)
    
    # update context with request and edited session data
    session.set_test_data(context, session_data)
    session.set_flow_data(context, constants.SESSION_DATA_TRIGGER_ACTION_REQ, request)

# ==============================================

@given('a valid session identifier to be redirected to WISP dismantling')
def get_valid_sessionid(context):

    session_id = session.get_flow_data(context, constants.SESSION_DATA_SESSION_ID)
    split_session_id = session_id.split("_")
    utils.assert_show_message(len(split_session_id[0]) == 11, f"The session ID must contains the broker code as first part of the session identifier.")
    utils.assert_show_message(len(split_session_id[1]) == 36, f"The session ID must contains an UUID as second part of the session identifier.")

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
        iuvs = []
    iuvs.insert(rpt_index, iuv)
    session.set_flow_data(context, constants.SESSION_DATA_IUVS, iuvs)

# ==============================================



# ==============================================
# ================ [WHEN] steps ================
# ==============================================

@when('the user sends a {primitive} action')
def send_nodoinviarpt(context, primitive):

    request = session.get_flow_data(context, constants.SESSION_DATA_TRIGGER_ACTION_REQ)
    url, subkey = router.get_soap_url(context, primitive)    
    headers = {'Content-Type': 'application/xml', 'SOAPAction': primitive, constants.OCP_APIM_SUBSCRIPTION_KEY: subkey}
    status_code, body_response, _ = utils.execute_request(url, "post", headers, request)

    session.set_flow_data(context, constants.SESSION_DATA_RES_CODE, status_code)
    session.set_flow_data(context, constants.SESSION_DATA_RES_BODY, body_response)
    
# ==============================================

@when('the user continue the session in WISP dismantling')
def send_sessionid_to_wispdismantling(context):

    session_data = session.get_test_data(context)

    url, _ = router.get_rest_url(context, "redirect")    
    headers = {'Content-Type': 'application/xml'}
    url += session.get_flow_data(context, constants.SESSION_DATA_SESSION_ID)
    status_code, _, response_headers = utils.execute_request(url, "get", headers)
    location_header = response_headers['Location']
    attach(location_header, name="Received response")

    session.set_flow_data(context, constants.SESSION_DATA_RES_CODE, status_code)
    session.set_flow_data(context, constants.SESSION_DATA_RES_BODY, location_header)

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
        url = base_url.format(
            creditor_institution=creditor_institution,
            iuv=iuv,
            date_from=today,
            date_to=today
        )
        status_code, body_response, _ = utils.execute_request(url, "get", headers, type=utils.ResponseType.JSON)
        utils.assert_show_message('data' in body_response, f"The response does not contains data.")
        utils.assert_show_message(len(body_response['data']) > 0, f"There are not event data in the response.")
        re_events.extend(body_response['data'])

    session.set_flow_data(context, constants.SESSION_DATA_RES_CODE, status_code)
    session.set_flow_data(context, constants.SESSION_DATA_RES_BODY, re_events)
    
# ==============================================



# ==============================================
# ================ [THEN] steps ================
# ==============================================

@then('the user receives the HTTP status code {status_code}')
def check_status_code(context, status_code):

    status_code = session.get_flow_data(context, constants.SESSION_DATA_RES_CODE)
    utils.assert_show_message(status_code == int(status_code), f"The status code is not 200. Current value: {status_code}.")

# ==============================================

@then('the user receives a response with outcome {outcome}')
def check_outcome(context, outcome):

    response = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    esito = response.find('.//esito')
    utils.assert_show_message(esito is not None, f"The field 'esito' in response does not exists.")
    utils.assert_show_message(esito.text == outcome, f"The field 'esito' in response is not {outcome}. Current value: {esito.text}.")

# ==============================================

@then('the user receives a response with the redirect URL')
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
def retrieve_notice_number_from_re_event(context):

    re_events = session.get_flow_data(context, constants.SESSION_DATA_RES_BODY)
    needed_events = [re_event for re_event in re_events if 'status' in re_event and re_event['status'] == "SAVED_RPT_IN_CART_RECEIVED_REDIRECT_URL_FROM_CHECKOUT"]
    utils.assert_show_message(len(needed_events) > 0, f"The redirect process is not ended successfully or there are missing events in RE")

    notice_numbers = set([re_event['noticeNumber'] for re_event in needed_events])
    utils.assert_show_message(len(notice_numbers) > 0, f"Impossible to extract notice numbers from events in RE")
    utils.assert_show_message(len(notice_numbers) == len(needed_events), f"Impossible to extract unique notice numbers from IUV codes")

    session.set_flow_data(context, constants.SESSION_DATA_NOTICE_NUMBERS, notice_numbers)

