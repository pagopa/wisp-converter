locals {

  apim_wisp_converter_rest_api = {
    project_name          = "wisp-converter"
    repo_name             = "pagopa-wisp-converter"
    display_name          = "WISP Converter"
    description           = "API for WISP Converter"
    path                  = "wisp-converter/api"
    subscription_required = true
    host                  = "api.${var.apim_dns_zone_prefix}.${var.external_domain}"
    hostname              = var.hostname
  }

}


# WISP Converter - REST APIs

resource "azurerm_api_management_api_version_set" "api_version_set_wisp_converter" {
  name                = "${var.prefix}-${var.env_short}-${var.location_short}-${local.apim_wisp_converter_rest_api.project_name}"
  resource_group_name = local.apim.rg
  api_management_name = local.apim.name
  display_name        = local.apim_wisp_converter_rest_api.display_name
  versioning_scheme   = "Segment"
}

module "wisp_converter_api_v1" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//api_management_api?ref=v6.7.0"

  name                  = format("%s-wisp-converter-api", var.env_short)
  api_management_name   = local.apim.name
  resource_group_name   = local.apim.rg
  product_ids           = [local.apim.product_id]
  subscription_required = true

  version_set_id = azurerm_api_management_api_version_set.api_version_set_wisp_converter.id
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