variable "deployment" {
  type = string
}

locals {
  qa_filter_prefix = join("\n", [
    "resource.labels.cluster_name=\"qa-private\"",
    "resource.labels.container_name=\"event-service\""
  ])

  stress_filter_prefix = join("\n", [
    "resource.labels.cluster_name=\"qa-stress\"",
    "resource.labels.container_name=\"event-service\""
  ])

  prod_filter_prefix = join("\n", [
    "resource.labels.cluster_name=\"prod-private-uswest1-primary\"",
    "resource.labels.container_name=\"event-service\"",
    "resource.labels.namespace_id=\"harness\""
  ])

  freemium_filter_prefix = join("\n", [
    "resource.type=\"container\"",
    "resource.labels.container_name=\"event-service\"",
    "resource.labels.namespace_id=\"harness-free\""
  ])

  filter_prefix = (var.deployment == "qa"     ? local.qa_filter_prefix :
                  (var.deployment == "stress" ? local.stress_filter_prefix :
                  (var.deployment == "prod"   ? local.prod_filter_prefix :
                                                local.freemium_filter_prefix)))

  name_prefix = join("_", ["x", var.deployment])
}
