variable "workflow-barrier" {}

locals {
    generic_instances = "${list(var.workflow-barrier ? "1" : "",
                                var.workflow-barrier ? "2" : "")}"
}

output "generic_instances" {
  value = "${compact(local.generic_instances)}"
}