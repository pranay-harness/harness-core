package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class TerrformStepConfiguration {
  @JsonProperty("type") TerraformStepConfigurationType terraformStepConfigurationType;
  @JsonProperty("spec") TerraformExecutionData terraformExecutionData;
}
