import json
import logging
import os
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


def before_all(context):

    # initialize precondition cache to avoid check systems up for each scenario
    context.precondition_cache = set()

    # configure logging setup
    logging.basicConfig(level=logging.DEBUG)

    # load configuration
    logging.debug('Global settings: loading configuration')
    other_data = json.load(open(os.path.join(context.config.base_dir + "/config/config.json")))
    for key, cfg in other_data.get("services").items():
        if cfg.get("subscription_key") is not None:
            cfg["subscription_key"] = os.getenv(cfg["subscription_key"])
    
    # load secrets
    logging.debug('Global settings: loading secrets')
    for key, value in other_data.get("secrets").items():
        other_data.get("secrets")[key] = os.getenv(value)

    # load common data
    logging.debug('Global settings: loading common data')
    common_data = json.load(open(os.path.join(context.config.base_dir + "/config/commondata.json")))
    items = [] 
    for key, value in common_data.get("test_data").items():
        if isinstance(value, str) and value.startswith("<"):
            injected_value = other_data.get("global_configuration").get(value[1:-1])
            if injected_value == None:
                injected_value = other_data.get("secrets").get(value[1:-1])
            common_data.get("test_data")[key] = injected_value
    
    for key, value in common_data.get("test_data").get("payee_institutions_1").items():
        if isinstance(value, str) and value.startswith("<"):
            injected_value = other_data.get("global_configuration").get(value[1:-1])
            if injected_value == None:
                injected_value = other_data.get("secrets").get(value[1:-1])
            common_data.get("test_data").get("payee_institutions_1")[key] = injected_value
    
    for key, value in common_data.get("test_data").get("payee_institutions_2").items():
        if isinstance(value, str) and value.startswith("<"):
            injected_value = other_data.get("global_configuration").get(value[1:-1])
            if injected_value == None:
                injected_value = other_data.get("secrets").get(value[1:-1])
            common_data.get("test_data").get("payee_institutions_2")[key] = injected_value
    
    for key, value in common_data.get("test_data").get("payee_institutions_3").items():
        if isinstance(value, str) and value.startswith("<"):
            injected_value = other_data.get("global_configuration").get(value[1:-1])
            if injected_value == None:
                injected_value = other_data.get("secrets").get(value[1:-1])
            common_data.get("test_data").get("payee_institutions_3")[key] = injected_value

    # update context data
    context.config.update_userdata(other_data)
    context.config.update_userdata(common_data)


def before_step(context, step):
    context.running_step = step.name