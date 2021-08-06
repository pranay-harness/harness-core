package io.harness.yaml.extended.ci.codebase;

import static io.harness.yaml.extended.ci.codebase.BuildTypeConstants.BRANCH_TYPE;
import static io.harness.yaml.extended.ci.codebase.BuildTypeConstants.TAG_TYPE;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("io.harness.yaml.extended.ci.BuildType")
public enum BuildType {
  @JsonProperty(BRANCH_TYPE) BRANCH(BRANCH_TYPE),
  @JsonProperty(TAG_TYPE) TAG(TAG_TYPE);

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
