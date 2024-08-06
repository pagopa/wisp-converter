import constants as const

# The method permits to retrieve the SOAP URL starting from request primitive
def get_primitive_url(context, primitive):
    services = context.config.userdata.get("services")
    match primitive.lower():
        case "nodoinviarpt":
            service_data = services.get("nodo-per-pa")
            return service_data['url'], service_data['subscription_key'], const.ResponseType.XML
        case "nodoinviacarrellorpt":
            service_data = services.get("nodo-per-pa")
            return service_data['url'], service_data['subscription_key'], const.ResponseType.XML
        case "checkposition":
            service_data = services.get("nodo-per-pm-v1")
            url = service_data['url'] + "/checkPosition"
            return url, service_data['subscription_key'], const.ResponseType.JSON
        case "activatepaymentnoticev2":
            service_data = services.get("node-for-psp")
            return service_data['url'], service_data['subscription_key'], const.ResponseType.XML
        case "closepaymentv2":
            service_data = services.get("nodo-per-pm-v2")
            url = service_data['url'] + "/closepayment"
            return url, service_data['subscription_key'], const.ResponseType.JSON
        case "sendpaymentoutcomev2":
            service_data = services.get("node-for-psp")
            return service_data['url'], service_data['subscription_key'], const.ResponseType.XML
        

# The method permits to retrieve the REST URL starting from action
def get_rest_url(context, action):
    services = context.config.userdata.get("services")
    match action.lower():
        case "redirect":
            service_data = services.get("wisp-converter")
            return service_data['url'] + "/payments?idSession=", ""
        case "search_in_re_by_iuv":            
            service_data = services.get("technical-support")
            url = service_data['url'] + "/organizations/{creditor_institution}/iuv/{iuv}?dateFrom={date_from}&dateTo={date_to}"
            return url, service_data['subscription_key']
        #case "search_in_re_by_nav":
        #    service_data = services.get("technical-support")
        #    url = service_data['url'] + "/organizations/{creditor_institution}/notice-number/{nav}?dateFrom={date_from}&dateTo={date_to}"
        #    return url, service_data['subscription_key']
        
