import re
import os
import requests
import logging
import datetime
import contextlib
import string
import random
from http.client import HTTPConnection

def obfuscate_secrets(request):
    return re.sub(r'', "<password>***</password>", request)
    


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


def get_global_conf(context, field):
    return context.config.userdata.get("global_configuration").get(field)


def replace_local_variables(payload, context):
    pattern = re.compile('\\$\\w+\\$')
    match = pattern.findall(payload)
    for field in match:
        value = getattr(context, field.replace('$', '').split('.')[0])
        payload = payload.replace(field, value)
    return payload


def replace_global_variables(payload, context):
    pattern = re.compile('#\\w+#')
    match = pattern.findall(payload)
    for field in match:
        name = field.replace('#', '').split('.')[0]
        if name in context.config.userdata.get("global_configuration"):
            value = get_global_conf(context, name)
            payload = payload.replace(field, value)
    return payload



def execute_request(url, method, headers, payload=None):
    debug_requests_on()
    req = requests.request(method=method, url=url, headers=headers, data=payload, verify=False)
    debug_requests_off()
    return req



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