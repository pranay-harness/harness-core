variable "credentialsFile" {
  type = string
}

variable "deployment" {
  type = string
}

variable "projectId" {
  type = string
}

variable "region" {
  type = string
}

provider "google" {
  credentials = var.credentialsFile
  project = var.projectId
}


module "ce-cloudfunctions" {
  source = "./ce"
  deployment = var.deployment
  projectId = var.projectId
  region = var.region
}

