/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

/**
 * The enum Infrastructure provisioner type.
 */
@OwnedBy(CDP) public enum InfrastructureProvisionerType { TERRAFORM, CLOUD_FORMATION, SHELL_SCRIPT, ARM, TERRAGRUNT }
