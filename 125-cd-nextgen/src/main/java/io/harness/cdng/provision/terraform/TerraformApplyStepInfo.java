package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.executions.steps.StepSpecTypeConstants.TERRAFORM_APPLY;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDP)
@JsonTypeName(TERRAFORM_APPLY)
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformApplyStepInfo {
  @JsonIgnore String name;
  @JsonIgnore String identifier;

  String provisionerIdentifier;
  @JsonProperty("configuration") TerrformStepConfiguration terrformStepConfiguration;
}
