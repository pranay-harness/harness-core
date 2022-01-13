/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
