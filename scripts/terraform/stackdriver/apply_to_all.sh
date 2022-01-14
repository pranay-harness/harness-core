#!/usr/bin/env bash
terraform workspace select freemium
terraform apply -input=false -auto-approve

terraform workspace select prod
terraform apply -input=false -auto-approve

terraform workspace select qa
terraform apply -input=false -auto-approve

terraform workspace select stress
terraform apply -input=false -auto-approve
