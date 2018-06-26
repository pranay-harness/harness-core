variable "user" {}
variable "access_key" {}
variable "secret_key" {}
variable "region" {
  default = "us-east-1"
}

variable "workflow-generic" {
  default = true
}

variable "workflow-barrier" {
  default = false
}

variable "workflow-collapse_nodes" {
  default = false
}

variable "workflow-terraform" {
  default = false
}

provider "aws" "aws_cloud" {
    version = "~> 1.0"

    access_key = "${var.access_key}"
    secret_key = "${var.secret_key}"
    region     = "${var.region}"
}

module "workflow-generic" {
  source  = "workflow-generic"

  workflow-generic = "${var.workflow-generic}"
}

module "workflow-barrier" {
  source  = "workflow-barrier"

  workflow-barrier = "${var.workflow-barrier}"
}

module "workflow-collapse_nodes" {
  source  = "workflow-collapse_nodes"

  workflow-collapse_nodes = "${var.workflow-collapse_nodes}"
}

module "workflow-terraform" {
  source  = "workflow-terraform"

  workflow-terraform = "${var.workflow-terraform}"
}


locals {
  generic-instances = "${distinct(concat(module.workflow-generic.generic_instances,
                                         module.workflow-barrier.generic_instances,
                                         module.workflow-collapse_nodes.generic_instances,
                                         module.workflow-terraform.generic_instances))}"
}

module "shared" {
  source  = "shared"

  user = "${var.user}"
  access_key = "${var.access_key}"
  secret_key = "${var.secret_key}"
  region = "${var.region}"

  generic-instances = "${length(local.generic-instances)}"
}

output "security_group" {
    value = "${module.shared.security_group}"
}

output "region" {
    value = "${var.region}"
}