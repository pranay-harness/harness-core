#!/usr/bin/env bash
terraform workspace select freemium
terraform apply

terraform workspace select prod
terraform apply

terraform workspace select qa
terraform apply

terraform workspace select stress
terraform apply
