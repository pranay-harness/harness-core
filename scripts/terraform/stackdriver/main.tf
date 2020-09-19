variable "credentialsFile" {
  type = string
}

variable "deployment" {
  type = string
}

variable "projectId" {
  type = string
}

provider "google" {
  credentials = var.credentialsFile
  project = var.projectId
}

module "manager" {
  source = "./manager"
  deployment = var.deployment
}

module "delegate-agent" {
  source = "./delegate-agent"
  deployment = var.deployment
  projectId = var.projectId
}

module "delegate-watcher" {
  source = "./delegate-watcher"
  deployment = var.deployment
}

module "event-service" {
  source = "./event-service"
  deployment = var.deployment
  projectId = var.projectId
}

module "batch-processing" {
  source = "./batch-processing"
  deployment = var.deployment
  projectId = var.projectId
}
  
module "mongo-dashboards" {
  source = "./mongo"
  deployment = var.deployment
}

module "ce-graphql" {
  source = "./ce-graphql"
  deployment = var.deployment
  projectId = var.projectId
}