variable "workflow-generic" {}

locals {
    generic_instances = "${list(var.workflow-generic ? "1" : "")}"
}

output "generic_instances" {
  value = "${compact(local.generic_instances)}"
}