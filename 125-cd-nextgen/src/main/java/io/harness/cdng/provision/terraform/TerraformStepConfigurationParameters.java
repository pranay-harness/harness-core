package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class TerraformStepConfigurationParameters {
  @NonNull TerraformStepConfigurationType type;
  TerraformExecutionDataParameters spec;
}
