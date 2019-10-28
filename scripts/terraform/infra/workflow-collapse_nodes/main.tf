variable "workflow-collapse_nodes" {}

locals {
    generic_instances = "${list(var.workflow-collapse_nodes ? "1" : "",
                                var.workflow-collapse_nodes ? "2" : "",
                                var.workflow-collapse_nodes ? "3" : "",
                                var.workflow-collapse_nodes ? "4" : "",
                                var.workflow-collapse_nodes ? "5" : "",
                                var.workflow-collapse_nodes ? "6" : "",
                                var.workflow-collapse_nodes ? "7" : "",
                                var.workflow-collapse_nodes ? "8" : "",
                                var.workflow-collapse_nodes ? "9" : "",
                                var.workflow-collapse_nodes ? "10" : "")}"
}

output "generic_instances" {
  value = "${compact(local.generic_instances)}"
}