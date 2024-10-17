resource "github_repository_environment" "github_repository_report_environment" {
  count               = var.env_short == "p" ? 1 : 0
  environment = "report-${var.env}"
  repository  = local.github.repository

  deployment_branch_policy {
    protected_branches     = var.github_repository_environment.protected_branches
    custom_branch_policies = var.github_repository_environment.custom_branch_policies
  }
}

locals {
  report_env_secrets = {
    "CD_CLIENT_ID" : data.azurerm_user_assigned_identity.identity_cd.client_id,
    "TENANT_ID" : data.azurerm_client_config.current.tenant_id,
    "SUBSCRIPTION_ID" : data.azurerm_subscription.current.subscription_id
    "REPORT_SLACK_WEBHOOK_URL" : data.azurerm_key_vault_secret.report_generation_slack_webhook_url.value,
    "REPORT_DATAEXPLORER_CLIENT_ID" : data.azurerm_key_vault_secret.report_generation_dataexplorer_clientid.value,
    "REPORT_DATAEXPLORER_CLIENT_SECRET" : data.azurerm_key_vault_secret.report_generation_dataexplorer_clientsecret.value,
    "REPORT_DATABASE_KEY" : data.azurerm_key_vault_secret.report_generation_database_key.value,
    "REPORT_APICONFIG_CACHE_SUBKEY" : data.azurerm_key_vault_secret.report_generation_apiconfigcache_subkey.value,
  }
  report_env_variables = {
    "CONTAINER_APP_ENVIRONMENT_NAME" : local.container_app_environment.name,
    "CONTAINER_APP_ENVIRONMENT_RESOURCE_GROUP_NAME" : local.container_app_environment.resource_group,
    "CLUSTER_NAME" : local.aks_cluster.name,
    "CLUSTER_RESOURCE_GROUP" : local.aks_cluster.resource_group_name,
    "DOMAIN" : local.domain,
    "NAMESPACE" : local.domain,
    "REPORT_DATAEXPLORER_URL": local.report_generation.dataexplorer_url,
    "REPORT_DATABASE_URL": local.report_generation.database_url,
    "REPORT_DATABASE_REGION": local.report_generation.database_region,
  }
}


###############
# ENV Secrets #
###############

resource "github_actions_environment_secret" "github_report_environment_runner_secrets" {
  for_each = { for k, v in local.report_env_secrets : k => v if var.env_short == "p" }
  repository  = local.github.repository
  environment = "report-${var.env}"
  secret_name = each.key
  plaintext_value = each.value
}

#################
# ENV Variables #
#################

resource "github_actions_environment_variable" "github_report_environment_runner_variables" {
  for_each = { for k, v in local.report_env_variables : k => v if var.env_short == "p" }
  repository    = local.github.repository
  environment = "report-${var.env}"
  variable_name = each.key
  value         = each.value
}
