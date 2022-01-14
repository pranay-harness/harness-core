package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

/**
 * The enum Infrastructure provisioner type.
 */
@OwnedBy(CDP) public enum InfrastructureProvisionerType { TERRAFORM, CLOUD_FORMATION, SHELL_SCRIPT, ARM, TERRAGRUNT }
