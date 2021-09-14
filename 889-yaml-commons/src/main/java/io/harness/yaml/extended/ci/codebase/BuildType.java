/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.extended.ci.codebase;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("io.harness.yaml.extended.ci.BuildType")
@OwnedBy(CI)
public enum BuildType {
  @JsonProperty(BuildTypeConstants.BRANCH) BRANCH(BuildTypeConstants.BRANCH),
  @JsonProperty(BuildTypeConstants.TAG) TAG(BuildTypeConstants.TAG),
  @JsonProperty(BuildTypeConstants.PR) PR(BuildTypeConstants.PR);

  private final String yamlProperty;

  @JsonCreator
  public static BuildType getCodeBaseType(@JsonProperty("type") String yamlPropertyName) {
    for (BuildType buildType : BuildType.values()) {
      if (buildType.yamlProperty.equalsIgnoreCase(yamlPropertyName)) {
        return buildType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + yamlPropertyName);
  }

  BuildType(String yamlProperty) {
    this.yamlProperty = yamlProperty;
  }

  @JsonValue
  public String getYamlProperty() {
    return yamlProperty;
  }

  @Override
  public String toString() {
    return yamlProperty;
  }

  public static BuildType fromString(final String s) {
    return BuildType.getCodeBaseType(s);
  }
}
