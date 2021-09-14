/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TimeSeriesThresholdActionType {
  IGNORE("ignore"),
  FAIL("fail");
  private String displayName;

  TimeSeriesThresholdActionType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @JsonCreator
  public static TimeSeriesThresholdActionType fromDisplayName(String displayName) {
    for (TimeSeriesThresholdActionType timeSeriesThresholdActionType : TimeSeriesThresholdActionType.values()) {
      if (timeSeriesThresholdActionType.displayName.equals(displayName)) {
        return timeSeriesThresholdActionType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + displayName);
  }
}
