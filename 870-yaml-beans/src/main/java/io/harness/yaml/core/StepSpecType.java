package io.harness.yaml.core;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.json.JsonOrchestrationIgnore;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface StepSpecType extends StepParameters {
  void setIdentifier(String identifier);
  void setName(String name);
  default void setDescription(ParameterField<String> description){};
  default void setSkipCondition(ParameterField<String> skipCondition){};

  @JsonIgnore StepType getStepType();
  @JsonIgnore String getFacilitatorType();

  @JsonIgnore
  @JsonOrchestrationIgnore
  default StepParameters getStepParameters() {
    return this;
  }

  @JsonIgnore
  @JsonOrchestrationIgnore
  default StepParameters getStepParametersWithNameAndIdentifier(
      String name, String identifier, ParameterField<String> description, ParameterField<String> skipCondition) {
    this.setIdentifier(identifier);
    this.setName(name);
    this.setDescription(description);
    this.setSkipCondition(skipCondition);
    return this;
  }
}
