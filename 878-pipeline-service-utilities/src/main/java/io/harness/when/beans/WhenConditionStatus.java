/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.when.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(PIPELINE)
public enum WhenConditionStatus {
  @JsonProperty(WhenConditionConstants.SUCCESS) SUCCESS(WhenConditionConstants.SUCCESS),
  @JsonProperty(WhenConditionConstants.FAILURE) FAILURE(WhenConditionConstants.FAILURE),
  @JsonProperty(WhenConditionConstants.ALL) ALL(WhenConditionConstants.ALL);

  private final String displayName;

  WhenConditionStatus(String displayName) {
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

  public static WhenConditionStatus getWhenConditionStatus(String displayName) {
    for (WhenConditionStatus value : WhenConditionStatus.values()) {
      if (value.getDisplayName().equals(displayName)) {
        return value;
      }
    }
    throw new InvalidArgumentsException("Invalid value: " + displayName);
  }
}
