package io.harness.yaml.core;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@OwnedBy(PIPELINE)
public interface StepSpecType extends StepParameters {
  @JsonIgnore StepType getStepType();
  @JsonIgnore String getFacilitatorType();

  @JsonIgnore
  default StepParameters getStepParameters() {
    return this;
  }

  @JsonIgnore
  default boolean skipUnresolvedExpressionsCheck() {
    return false;
  }
}
