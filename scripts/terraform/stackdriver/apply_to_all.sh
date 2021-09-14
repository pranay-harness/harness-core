#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

terraform workspace select freemium
terraform apply -input=false -auto-approve

terraform workspace select prod
terraform apply -input=false -auto-approve

terraform workspace select qa
terraform apply -input=false -auto-approve

terraform workspace select stress
terraform apply -input=false -auto-approve
