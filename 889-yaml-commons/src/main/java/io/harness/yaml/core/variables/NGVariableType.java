/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.core.variables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NGVariableType {
  @JsonProperty(NGVariableConstants.STRING_TYPE) STRING(NGVariableConstants.STRING_TYPE),
  @JsonProperty(NGVariableConstants.NUMBER_TYPE) NUMBER(NGVariableConstants.NUMBER_TYPE),
  @JsonProperty(NGVariableConstants.SECRET_TYPE) SECRET(NGVariableConstants.SECRET_TYPE);

  private final String yamlProperty;

  NGVariableType(String yamlProperty) {
    this.yamlProperty = yamlProperty;
  }

  @JsonValue
  public String getYamlProperty() {
    return yamlProperty;
  }

  @JsonCreator
  public static NGVariableType getNGVariableType(@JsonProperty("type") String yamlProperty) {
    for (NGVariableType ngVariableType : NGVariableType.values()) {
      if (ngVariableType.getYamlProperty().equalsIgnoreCase(yamlProperty)) {
        return ngVariableType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + yamlProperty);
  }
}
