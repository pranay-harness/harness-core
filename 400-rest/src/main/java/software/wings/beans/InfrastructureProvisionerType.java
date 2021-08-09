package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

/**
 * The enum Infrastructure provisioner type.
 */
@OwnedBy(CDP)
@TargetModule(HarnessModule._959_CG_BEANS)
public enum InfrastructureProvisionerType {
  TERRAFORM,
  CLOUD_FORMATION,
  SHELL_SCRIPT,
  ARM,
  TERRAGRUNT
}
