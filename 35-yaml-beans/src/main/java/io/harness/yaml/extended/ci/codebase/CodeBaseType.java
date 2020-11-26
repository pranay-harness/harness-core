package io.harness.yaml.extended.ci.codebase;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("io.harness.yaml.extended.ci.CodeBaseType")
public enum CodeBaseType {
  @JsonProperty("GitHub") GIT_HUB("GitHub");

  private final String displayName;

  @JsonCreator
  public static CodeBaseType getCodeBaseType(@JsonProperty("type") String displayName) {
    for (CodeBaseType connectorType : CodeBaseType.values()) {
      if (connectorType.displayName.equalsIgnoreCase(displayName)) {
        return connectorType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + displayName);
  }

  CodeBaseType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }

  public static CodeBaseType fromString(final String s) {
    return CodeBaseType.getCodeBaseType(s);
  }
}
