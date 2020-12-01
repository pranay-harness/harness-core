package io.harness.pms.yaml.core.variables;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.yaml.core.variables.NGVariableType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "type", include = PROPERTY, visible = true)
public interface NGVariablePms {
  NGVariableType getType();
  String getName();
  String getDescription();
  boolean isRequired();
}
