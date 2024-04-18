locals {
  project_name = "wisp-converter"
  repo_name    = "pagopa-wisp-converter"

  display_name = "WISP Converter"
  description  = "WISP Converter services"
  path         = "wisp-converter/api"

  host     = "api.${var.apim_dns_zone_prefix}.${var.external_domain}"
  hostname = var.hostname
}

resource "azurerm_api_management_api_version_set" "api_version_set_wisp_converter" {
  name                = "${var.prefix}-${var.env_short}-${var.location_short}-${local.project_name}"
  resource_group_name = local.apim.rg
  api_management_name = local.apim.name
  display_name        = local.display_name
  versioning_scheme   = "Segment"
}

module "wisp_converter_api_v1" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//api_management_api?ref=v6.7.0"

  name                  = format("%s-api", local.project)
  api_management_name   = local.apim.name
  resource_group_name   = local.apim.rg
  product_ids           = [local.apim.product_id]
  subscription_required = true

  version_set_id = azurerm_api_management_api_version_set.api_version_set_wisp_converter.id
  api_version    = "v1"

  description  = local.description
  display_name = local.display_name
  path         = local.path
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
