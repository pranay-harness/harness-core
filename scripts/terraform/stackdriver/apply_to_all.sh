#!/usr/bin/env bash
# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

terraform workspace select freemium
terraform apply -input=false -auto-approve

terraform workspace select prod
terraform apply -input=false -auto-approve

terraform workspace select qa
terraform apply -input=false -auto-approve

terraform workspace select stress
terraform apply -input=false -auto-approve
