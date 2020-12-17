package io.harness.pms.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(CDC)
public interface OrchestrationField {
  String ORCHESTRATION_FIELD_CLASS_FIELD = "_orchestrationFieldClass";

  @JsonProperty(ORCHESTRATION_FIELD_CLASS_FIELD) Class<? extends OrchestrationField> getDeserializationClass();
  OrchestrationFieldType getType();
  Object fetchFinalValue();
}
