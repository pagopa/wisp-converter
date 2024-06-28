locals {
  repo_name    = "pagopa-wisp-converter"
  host     = "api.${var.apim_dns_zone_prefix}.${var.external_domain}"
  hostname = var.hostname

  wisp_converter_internal = {
    project_name = "wisp-converter"
    display_name = "WISP Converter"
    description  = "WISP Converter services"
    path         = "wisp-converter/api"
  }

  wisp_converter_redirect = {
    project_name = "wisp-converter-redirect"
    display_name = "WISP Converter - Redirect"
    description  = "API for redirect payments handled by WISP to eCommerce"
    path         = "wisp-converter/redirect/api"
  }
}

/**************************
WISP-Converter API Internal
***************************/

resource "azurerm_api_management_api_version_set" "api_version_set_wisp_converter" {
  name                = "${var.prefix}-${var.env_short}-${var.location_short}-${local.wisp_converter_internal.project_name}"
  resource_group_name = local.apim.rg
  api_management_name = local.apim.name
  display_name        = local.wisp_converter_internal.display_name
  versioning_scheme   = "Segment"
}

module "wisp_converter_api_v1" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//api_management_api?ref=v6.7.0"

  name                  = format("%s-api", local.wisp_converter_internal.project_name)
  api_management_name   = local.apim.name
  resource_group_name   = local.apim.rg
  product_ids           = [local.apim.product_id]
  subscription_required = true

  version_set_id = azurerm_api_management_api_version_set.api_version_set_wisp_converter.id
  api_version    = "v1"

  description  = local.wisp_converter_internal.description
  display_name = local.wisp_converter_internal.display_name
  path         = local.wisp_converter_internal.path
  protocols    = ["https"]

  service_url = null

  content_format = "openapi"
  content_value  = templatefile("../openapi/openapi.json", {
    host = local.host
  })

  xml_content = templatefile("./policy/_base_policy.xml", {
    hostname = var.hostname
  })
}

/**************************
WISP-Converter API Redirect
***************************/

resource "azurerm_api_management_api_version_set" "api_version_set_wisp_converter_redirect" {
  name                = "${var.prefix}-${var.env_short}-${var.location_short}-${local.wisp_converter_redirect.project_name}"
  resource_group_name = local.apim.rg
  api_management_name = local.apim.name
  display_name        = local.wisp_converter_redirect.display_name
  versioning_scheme   = "Segment"
}

module "wisp_converter_redirect_api_v1" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//api_management_api?ref=v6.7.0"

  name                  = format("%s-api", local.wisp_converter_redirect.project_name)
  api_management_name   = local.apim.name
  resource_group_name   = local.apim.rg
  product_ids           = [local.apim.product_id]
  subscription_required = false

  version_set_id = azurerm_api_management_api_version_set.api_version_set_wisp_converter_redirect.id
  api_version    = "v1"

  description  = local.wisp_converter_redirect.description
  display_name = local.wisp_converter_redirect.display_name
  path         = local.wisp_converter_redirect.path
  protocols    = ["https"]

  service_url = null

  content_format = "openapi"
  content_value  = templatefile("../openapi/openapi.json", {
    host = local.host
  })

  xml_content = templatefile("./policy/_base_policy.xml", {
    hostname = var.hostname
  })
}
