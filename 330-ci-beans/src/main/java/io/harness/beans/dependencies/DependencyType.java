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
