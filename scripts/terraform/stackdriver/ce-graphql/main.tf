variable "deployment" {
  type = string
}

variable "projectId" {
  type = string
}

locals {
  qa_filter_prefix = join("\n", [
    "resource.type=\"k8s_container\"",
    "resource.labels.cluster_name=\"qa-private\"",
    "resource.labels.container_name=\"manager-qa\"",
    "resource.labels.namespace_name:\"manager-qa\""
  ])

  qa_free_filter_prefix = join("\n", [
    "resource.type=\"k8s_container\"",
    "resource.labels.cluster_name=\"qa-private\"",
    "resource.labels.container_name=\"manager-qa-free\"",
    "resource.labels.namespace_name:\"manager-free\""
  ])

  stress_filter_prefix = join("\n", [
    "resource.type=\"k8s_container\"",
    "resource.labels.cluster_name=\"qa-stress\"",
    "resource.labels.container_name=\"manager-stress\"",
  ])

  prod_filter_prefix = join("\n", [
    "resource.type=\"k8s_container\"",
    "resource.labels.cluster_name=\"prod-private-uswest1-primary\"",
    "resource.labels.container_name=\"manager\"",
    "resource.labels.namespace_name:\"manager-prod-\""
  ])

  prod_failover_filter_prefix = join("\n", [
    "resource.type=\"k8s_container\"",
    "resource.labels.cluster_name=\"prod-private-uswest2-failover\"",
    "resource.labels.container_name=\"manager\"",
    "resource.labels.namespace_name:\"manager-prod-\""
  ])

  prod_freemium_filter_prefix = join("\n", [
    "resource.type=\"k8s_container\"",
    "resource.labels.cluster_name=\"prod-private-uswest1-primary\"",
    "resource.labels.container_name=\"manager\"",
    "resource.labels.namespace_name=\"harness-free\""
  ])

  dev_filter_prefix = join("\n", [
    "resource.type=\"k8s_container\"",
    "resource.labels.cluster_name=\"ce-dev\"",
    "resource.labels.container_name=\"manager-dev\"",
    "resource.labels.namespace_name:\"manager-\""
  ])

  filter_prefix = (var.deployment == "qa" ? local.qa_filter_prefix :
    (var.deployment == "qa-free" ? local.qa_free_filter_prefix :
      (var.deployment == "dev" ? local.dev_filter_prefix :
        (var.deployment == "stress" ? local.stress_filter_prefix :
          (var.deployment == "prod" ? local.prod_filter_prefix :
            (var.deployment == "freemium" ? local.prod_freemium_filter_prefix :
              (var.deployment == "prod-failover" ? local.prod_failover_filter_prefix :
  local.qa_filter_prefix)))))))

  name_prefix = join("_", ["x", var.deployment])

  # prod and freemium is #ce-alerts channel. dev and qa is #ce-alerts-test channel
  slack_prod_channel = "projects/${var.projectId}/notificationChannels/10185135917587539827"
  slack_dev_channel = "projects/${var.projectId}/notificationChannels/13768296773189683769"
  slack_qa_channel = "projects/${var.projectId}/notificationChannels/9145672106555378098"
}