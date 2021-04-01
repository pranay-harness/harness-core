package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.executions.steps.StepSpecTypeConstants.TERRAFORM_PLAN;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@JsonTypeName(TERRAFORM_PLAN)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformPlanStepInfo {
  @JsonIgnore String name;
  @JsonIgnore String identifier;

  String provisionerIdentifier;
  @JsonProperty("configuration") TerraformPlanExecutionData terraformPlanExecutionData;
}
