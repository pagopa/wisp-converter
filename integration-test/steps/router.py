
# The method permits to retrieve the SOAP URL starting from request primitive
def get_soap_url(context, primitive):
    services = context.config.userdata.get("services")
    match primitive.lower():
        case "nodoinviarpt":
            service_data = services.get("nodo-per-pa")
            return service_data['url'], service_data['subscription_key']
        case "nodoinviacarrellorpt":
            service_data = services.get("nodo-per-pa")
            return service_data['url'], service_data['subscription_key']
        

# The method permits to retrieve the REST URL starting from action
def get_rest_url(context, action):
    services = context.config.userdata.get("services")
    match action.lower():
        case "redirect":
            service_data = services.get("wisp-converter")
            return service_data['url'] + "/payments?idSession=", ""
        
