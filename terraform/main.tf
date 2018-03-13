variable "user" {}
variable "access_key" {}
variable "secret_key" {}
variable "region" {
  default = "us-east-1"
}

provider "aws" {
    version = "~> 1.0"

    access_key = "${var.access_key}"
    secret_key = "${var.secret_key}"
    region     = "${var.region}"
}

module "shared" {
  source  = "shared"
}

module "test-workflow" {
  source  = "test-workflow"

  user = "${var.user}"
  aws_key_pair_id = "${module.shared.aws_key_pair_id}"
  aws_security_group_name = "${module.shared.aws_security_group_name}"
}
