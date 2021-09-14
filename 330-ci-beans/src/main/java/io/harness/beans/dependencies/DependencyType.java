/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.dependencies;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DependencyType {
  @JsonProperty(DependencyConstants.SERVICE_TYPE) SERVICE(DependencyConstants.SERVICE_TYPE);

  private final String yamlProperty;

  DependencyType(String yamlProperty) {
    this.yamlProperty = yamlProperty;
  }

  @JsonValue
  public String getYamlProperty() {
    return yamlProperty;
  }

  @JsonCreator
  public static DependencyType getDependencyType(@JsonProperty("type") String yamlProperty) {
    for (DependencyType dependencyType : DependencyType.values()) {
      if (dependencyType.getYamlProperty().equalsIgnoreCase(yamlProperty)) {
        return dependencyType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + yamlProperty);
  }
}
