/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExecutionStrategyType {
  @JsonProperty("Basic") BASIC("Basic"),
  @JsonProperty("Canary") CANARY("Canary"),
  @JsonProperty("BlueGreen") BLUE_GREEN("BlueGreen"),
  @JsonProperty("Rolling") ROLLING("Rolling"),
  @JsonProperty("Default") DEFAULT("Default");
  ;

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
