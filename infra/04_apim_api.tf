locals {

  apim_wisp_converter_soap_api = {
    project_name          = "wisp-converter-soap"
    repo_name             = "pagopa-wisp-converter"
    display_name          = "WISP Converter - SOAP"
    description           = "API SOAP for adapting WISP-made requests for GPD system"
    path                  = "wisp-converter/service"
    subscription_required = true
    host                  = "api.${var.apim_dns_zone_prefix}.${var.external_domain}"
    hostname              = var.hostname
  }
  apim_wisp_converter_rest_api = {
    project_name          = "wisp-converter-rest"
    repo_name             = "pagopa-wisp-converter"
    display_name          = "WISP Converter"
    description           = "API for WISP Converter"
    path                  = "wisp-converter/api"
    subscription_required = true
    host                  = "api.${var.apim_dns_zone_prefix}.${var.external_domain}"
    hostname              = var.hostname
  }
}


# WISP Converter - SOAP APIs

resource "azurerm_api_management_api_version_set" "api_version_set_wisp_converter_soap" {
  name                = "${var.prefix}-${var.env_short}-${var.location_short}-${local.apim_wisp_converter_soap_api.project_name}"
  resource_group_name = local.apim.rg
  api_management_name = local.apim.name
  display_name        = local.apim_wisp_converter_soap_api.display_name
  versioning_scheme   = "Segment"
}


module "wisp_converter_soap_api_v1" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//api_management_api?ref=v6.7.0"

  name                  = format("%s-api-gpd-payments-soap-api", var.env_short)
  api_management_name   = local.apim.name
  resource_group_name   = local.apim.rg
  subscription_required = local.apim_wisp_converter_soap_api.subscription_required
  version_set_id        = azurerm_api_management_api_version_set.api_version_set_wisp_converter_soap.id
  version               = "v1"

  description  = local.apim_wisp_converter_soap_api.description
  display_name = local.apim_wisp_converter_soap_api.display_name
  path         = local.apim_wisp_converter_soap_api.path
  protocols    = ["https"]
  service_url  = format("https://%s/pagopa-wisp-converter/service", local.apim_wisp_converter_soap_api.hostname)

  soap_pass_through = true

  import {
    content_format = "wsdl"
    content_value  = file("./soap/TODO.wsdl") // TODO set WSDL
    wsdl_selector {
      service_name  = "TODO_Service"
      endpoint_name = "TODO_Port"
    }
  }
}


# WISP Converter - REST APIs

resource "azurerm_api_management_api_version_set" "api_version_set_wisp_converter_rest" {
  name                = "${var.prefix}-${var.env_short}-${var.location_short}-${local.apim_wisp_converter_rest_api.project_name}"
  resource_group_name = local.apim.rg
  api_management_name = local.apim.name
  display_name        = local.apim_wisp_converter_rest_api.display_name
  versioning_scheme   = "Segment"
}

module "wisp_converter_rest_api_v1" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//api_management_api?ref=v6.7.0"

  name                  = format("%s-wisp-converter-rest-api", var.env_short)
  api_management_name   = local.apim.name
  resource_group_name   = local.apim.rg
  product_ids           = [local.apim.product_id]
  subscription_required = true

  version_set_id = azurerm_api_management_api_version_set.api_version_set_wisp_converter_rest.id
  api_version    = "v1"

  description  = local.apim_wisp_converter_rest_api.description
  display_name = local.apim_wisp_converter_rest_api.display_name
  path         = local.apim_wisp_converter_rest_api.path
  protocols    = ["https"]

  service_url = null

  content_format = "openapi"
  content_value  = templatefile("../openapi/openapi.json", {
    host = local.apim_wisp_converter_rest_api.host
  })

  xml_content = templatefile("./policy/_base_policy.xml", {
    hostname = var.hostname
  })
}
