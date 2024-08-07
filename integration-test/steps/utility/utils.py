from enum import Enum
import json
import re
import requests
import logging
import datetime
import string
import random
from allure_commons._allure import attach
from allure import attachment_type
import xml.etree.ElementTree as xmlutils

import constants as constants


# ==============================================

def execute_request(url, method, headers, payload=None, type=constants.ResponseType.XML, allow_redirect=True):
    if payload is not None:
        attach(obfuscate_secrets("URL: " + url + "\nRequest:\n" + payload), name=f'{url} - Sent request', attachment_type=attachment_type.TEXT)
        
    response = requests.request(method=method, url=url, headers=headers, data=payload, verify=False, allow_redirects=allow_redirect)
    response_text = response.text
    object_response = None
    if response.text is not None and len(response.text) > 0:        
        
        if type == constants.ResponseType.XML:
            formatted_response = remove_namespace(response.text)
            attach(obfuscate_secrets("URL: " + url + "\nResponse:\n" + formatted_response), name=f'{url} - Received response', attachment_type=attachment_type.TEXT)
            if formatted_response is not None:
                object_response = xmlutils.fromstring(formatted_response)
        elif type == constants.ResponseType.JSON:
            object_response = response.json()
            attach(obfuscate_secrets("URL: " + url + "\nResponse:\n" +  json.dumps(object_response, indent=2)), name=f'{url} - Received response',  attachment_type=attachment_type.TEXT)
    
    return response.status_code, object_response, response.headers

# ==============================================

def get_nested_field(object, field_name):
    try:   
        nested_fields = field_name.split('.')
        analyzed_object = object    
        for field in nested_fields:
            if field in analyzed_object:
                analyzed_object = analyzed_object[field]
            else:
                return None
        return analyzed_object
    except Exception as e:
        return None 

# ==============================================

def obfuscate_secrets(request):
    request_without_secrets = re.sub(r'<password>(.*)<\/password>', "<password>***</password>", request)
    request_without_secrets = re.sub(r'\"password\":\s{0,1}\"(.*)\"', "\"password\": \"***\"", request_without_secrets)
    return request_without_secrets

# ==============================================

def remove_namespace(content):
    content_without_ns = re.sub(r'(<\/?)(\w+:)', r'\1', content)
    content_without_ns = re.sub(r'\sxmlns[^"]+"[^"]+"', '', content_without_ns)
    return content_without_ns
  
# ==============================================
  
def generate_iuv():
    return get_random_digit_string(15)

# ==============================================

def generate_ccp():
    return get_random_digit_string(16)

# ==============================================

def get_random_digit_string(length):
    return ''.join(random.choice(string.digits) for i in range(length))

# ==============================================

def get_random_alphanumeric_string(length):
    return ''.join(random.choice(string.ascii_letters + string.digits) for i in range(length))

# ==============================================

def get_index_from_cardinal(cardinal):    
    index = -1
    match cardinal:
        case "first":
            index = 0
        case "second":
            index = 1
        case "third":
            index = 2
        case "fourth":
            index = 3
        case "fifth":
            index = 4
    return index

# ==============================================

def generate_random_monetary_amount(min, max):    
    random_amount = random.uniform(min, max)
    return round(random_amount, 2)

# ==============================================

def get_current_datetime():
    today = datetime.datetime.today().astimezone()
    return today.strftime("%Y-%m-%dT%H:%M:%S")

# ==============================================

def get_current_date():
    today = datetime.datetime.today().astimezone()
    return today.strftime("%Y-%m-%d")

# ==============================================

def assert_show_message(assertion_value, message):
    try:
        assert assertion_value, message
    except AssertionError as e:
        logging.error(f"[Assert Error] {e}")
        raise

# ==============================================
