variable "workflow-terraform" {}

locals {
    generic_instances = "${list(var.workflow-terraform ? "1" : "")}"
}

output "generic_instances" {
  value = "${compact(local.generic_instances)}"
}