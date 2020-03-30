variable "credentialsFile" {
  type    = "string"
}

variable "deployment" {
  type = "string"
}

variable "projectId" {
  type = "string"
}

provider "google" {
  credentials = var.credentialsFile
  project = var.projectId
}

locals {
  qa_filter_prefix = join("\n", [
    "resource.labels.cluster_name=\"qa-private\"",
    "resource.labels.container_name=\"manager-qa\""
  ])

  prod_filter_prefix = join("\n", [
    "resource.labels.cluster_name=\"prod-private-uswest1-primary\"",
    "resource.labels.container_name=\"manager\"",
    "resource.labels.namespace_id:\"manager-prod-\""
  ])

  freemium_filter_prefix = join("\n", [
    "resource.type=\"container\"",
    "resource.labels.container_name=\"manager\"",
    "resource.labels.namespace_id:\"manager-free-\""
  ])

  filter_prefix = (var.deployment == "qa"   ? local.qa_filter_prefix :
                  (var.deployment == "prod" ? local.prod_filter_prefix :
                                              local.freemium_filter_prefix))

  name_prefix = join("_", ["x", var.deployment])
}