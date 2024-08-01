from enum import Enum
import re
import requests
import logging
import datetime
import string
import random
from http.client import HTTPConnection
from allure_commons._allure import attach
import xml.etree.ElementTree as xmlutils

import constants as constants

class ResponseType(Enum):
    XML = 1
    JSON = 2

   
def debug_requests_on():
    '''Switches on logging of the requests module.'''
    HTTPConnection.debuglevel = 1

    logging.basicConfig()
    logging.getLogger().setLevel(logging.DEBUG)
    requests_log = logging.getLogger("requests.packages.urllib3")
    requests_log.setLevel(logging.DEBUG)
    requests_log.propagate = True


def debug_requests_off():
    '''Switches off logging of the requests module, might be some side-effects'''
    HTTPConnection.debuglevel = 0

    root_logger = logging.getLogger()
    root_logger.setLevel(logging.WARNING)
    root_logger.handlers = []
    requests_log = logging.getLogger("requests.packages.urllib3")
    requests_log.setLevel(logging.WARNING)
    requests_log.propagate = False


def execute_request(url, method, headers, payload=None, type=ResponseType.XML):
    if payload is not None:
        attach(obfuscate_secrets(payload), name="Sent request")
    #debug_requests_on()
    response = requests.request(method=method, url=url, headers=headers, data=payload, verify=False, allow_redirects=False)
    #debug_requests_off()
    object_response = None
    if response.text is not None and len(response.text) > 0:
        formatted_response = remove_namespace(response.text)
        attach(obfuscate_secrets(formatted_response), name="Received response")
        
        if type == ResponseType.XML:
            object_response = xmlutils.fromstring(formatted_response)
        elif type == ResponseType.JSON:
            object_response = response.json()
        
    return response.status_code, object_response, response.headers


def obfuscate_secrets(request):
    request_without_secrets = re.sub(r'<password>(.*)<\/password>', "<password>***</password>", request)
    request_without_secrets = re.sub(r'\"password\":\s{0,1}\"(.*)\"', "\"password\": \"***\"", request_without_secrets)
    return request_without_secrets


def remove_namespace(content):
    content_without_ns = re.sub(r'(<\/?)(\w+:)', r'\1', content)
    content_without_ns = re.sub(r'\sxmlns[^"]+"[^"]+"', '', content_without_ns)
    return content_without_ns
    

def generate_iuv():
    return get_random_digit_string(15)


def generate_ccp():
    return get_random_digit_string(16)


def get_random_digit_string(length):
    return ''.join(random.choice(string.digits) for i in range(length))


def generate_random_monetary_amount(min, max):    
    random_amount = random.uniform(min, max)
    return round(random_amount, 2)


def get_current_datetime():
    today = datetime.datetime.today().astimezone()
    return today.strftime("%Y-%m-%dT%H:%M:%S")


def get_current_date():
    today = datetime.datetime.today().astimezone()
    return today.strftime("%Y-%m-%d")


def assert_show_message(assertion_value, message):
    try:
        assert assertion_value, message
    except AssertionError as e:
        logging.error(f"[Assert Error] {e}")
        raise