package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExecutionStrategyType {
  @JsonProperty("Basic") BASIC("Basic"),
  @JsonProperty("Canary") CANARY("Canary"),
  @JsonProperty("BlueGreen") BLUE_GREEN("BlueGreen"),
  @JsonProperty("Rolling") ROLLING("Rolling");

  private String displayName;

  @JsonCreator
  public static ExecutionStrategyType getExecutionStrategy(@JsonProperty("type") String displayName) {
    for (ExecutionStrategyType executionStrategyType : ExecutionStrategyType.values()) {
      if (executionStrategyType.displayName.equalsIgnoreCase(displayName)) {
        return executionStrategyType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + displayName);
  }

  ExecutionStrategyType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  public static ExecutionStrategyType fromString(final String s) {
    return ExecutionStrategyType.getExecutionStrategy(s);
  }
}
