import copy
import re
from urllib.parse import parse_qs, urlparse
from behave import *
from datetime import timezone, timedelta
import logging
import requests
from allure_commons._allure import attach
import behave as behave
import xml.etree.ElementTree as xmlutils

import utils as utils
import request_generator as requestgen
import constants as constants
import router as router




@given('systems up')
def step_impl(context):
    
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








@given('a single RPT with {number_of_transfers} transfers')
def generate_single_rpt(context, number_of_transfers):

    session_data = copy.deepcopy(context.config.userdata.get("test_data"))
    session_data = requestgen.create_transferset(session_data, int(number_of_transfers))

    # generate request
    request = requestgen.generate_rpt(session_data)
    
    # update context with request and edited session data
    setattr(context, constants.REQUEST_FROM_TRIGGER_ACTION, request)
    setattr(context, constants.SESSION_DATA, session_data)



@given('a valid session identifier to be redirected to WISP dismantling')
def validate_sessionid(context):

    session_data = getattr(context, constants.SESSION_DATA)
    session_id = session_data[constants.SESSION_ID]
    split_session_id = session_id.split("_")
    utils.assert_show_message(len(split_session_id[0]) == 11, f"The session ID must contains the broker code as first part of the session identifier.")
    utils.assert_show_message(len(split_session_id[1]) == 36, f"The session ID must contains an UUID as second part of the session identifier.")



@when('the user sends a {primitive} action')
def send_nodoinviarpt(context, primitive):

    request = getattr(context, constants.REQUEST_FROM_TRIGGER_ACTION)
    url, subkey = router.get_soap_url(context, primitive)    
    headers = {'Content-Type': 'application/xml', 'SOAPAction': primitive, constants.OCP_APIM_SUBSCRIPTION_KEY: subkey}
    
    attach(request, name="Sent request")
    soap_response = requests.post(url, request, headers=headers, verify=False)
    response = re.sub(r'(<\/?)(\w+:)', r'\1', soap_response.text)
    response = re.sub(r'\sxmlns[^"]+"[^"]+"', '', response)
    attach(response, name="Received response")

    setattr(context, constants.RESPONSE_STATUS_FROM_TRIGGER_ACTION, soap_response.status_code)
    setattr(context, constants.RESPONSE_BODY_FROM_TRIGGER_ACTION, xmlutils.fromstring(response))
    

@when('the user continue the session in WISP dismantling')
def send_sessionid_to_wispdismantling(context):

    session_data = getattr(context, constants.SESSION_DATA)

    url, _ = router.get_rest_url(context, "redirect")    
    headers = {'Content-Type': 'application/xml'}
    url = url + session_data[constants.SESSION_ID]

    attach(url, name="Sent request at URL")
    response = requests.get(url, headers=headers, verify=False, allow_redirects=False)
    location_header = response.headers['Location']
    attach(location_header, name="Received response")

    setattr(context, constants.RESPONSE_STATUS, response.status_code)
    setattr(context, constants.RESPONSE_BODY, location_header)










@then('the user receives the HTTP status code {status_code}')
def check_status_code(context, status_code):

    status_code = getattr(context, constants.RESPONSE_STATUS_FROM_TRIGGER_ACTION)
    utils.assert_show_message(status_code == int(status_code), f"The status code is not 200. Current value: {status_code}.")


@then('the user receives a response with outcome {outcome}')
def check_outcome(context, outcome):

    response = getattr(context, constants.RESPONSE_BODY_FROM_TRIGGER_ACTION)
    esito = response.find('.//esito')
    utils.assert_show_message(esito is not None, f"The field 'esito' in response does not exists.")
    utils.assert_show_message(esito.text == outcome, f"The field 'esito' in response is not {outcome}. Current value: {esito.text}.")


@then('the user receives a response with the redirect URL')
def check_redirect_url(context):

    response = getattr(context, constants.RESPONSE_BODY_FROM_TRIGGER_ACTION)
    redirect_url = response.find('.//url')
    utils.assert_show_message(redirect_url is not None, f"The field 'redirect_url' in response doesn't exists.")

    parsed_url = urlparse(redirect_url.text)
    query_params = parse_qs(parsed_url.query)
    id_session = query_params['idSession'][0] if len(query_params['idSession']) > 0 else None
    utils.assert_show_message(id_session is not None, f"The field 'idSession' in response is not correctly set.")

    session_data = getattr(context, constants.SESSION_DATA)
    session_data[constants.SESSION_ID] = id_session
    setattr(context, constants.SESSION_DATA, session_data)


@then('the user can be redirected to Checkout')
def check_redirect_url(context):

    location_redirect_url = getattr(context, constants.RESPONSE_BODY)    
    utils.assert_show_message(location_redirect_url is not None, f"The header 'Location' does not exists.")
    utils.assert_show_message("ecommerce/checkout" in location_redirect_url, f"The header 'Location' does not refers to Checkout service. {location_redirect_url}")
    

