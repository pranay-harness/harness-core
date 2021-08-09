# Using multiple workspaces:
terraform {
  backend "remote" {
    hostname = "app.terraform.io"
    organization = "Harness-DevOps"

    workspaces {
      prefix = "ce-"
    }
  }
}
