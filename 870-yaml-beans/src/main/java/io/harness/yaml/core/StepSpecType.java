package io.harness.yaml.core;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.json.JsonOrchestrationIgnore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface StepSpecType extends StepParameters {
  void setIdentifier(String identifier);
  void setName(String name);

  @JsonIgnore StepType getStepType();
  @JsonIgnore String getFacilitatorType();

  @JsonIgnore
  @JsonOrchestrationIgnore
  default StepParameters getStepParameters() {
    return this;
  }
}
