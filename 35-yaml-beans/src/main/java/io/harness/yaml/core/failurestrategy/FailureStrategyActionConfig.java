package io.harness.yaml.core.failurestrategy;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "type", include = PROPERTY, visible = true)
public interface FailureStrategyActionConfig {
  @JsonIgnore NGFailureActionType getType();
}
