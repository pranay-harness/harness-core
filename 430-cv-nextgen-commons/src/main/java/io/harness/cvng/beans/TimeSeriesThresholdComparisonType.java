/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Possible types of thresholds that can be applied to a metric.
 * Created by mike@ on 6/19/17.
 */
public enum TimeSeriesThresholdComparisonType {
  /**
   * A threshold that is divided by the previous build's value to yield a ratio.
   */
  RATIO("ratio"),
  /**
   * A threshold that is subtracted from the previous build's value to yield a delta.
   */
  DELTA("delta"),
  /**
   * A threshold that represents an absolute value to compare against rather than something in the previous build.
   */
  ABSOLUTE("absolute-value");

  private String displayName;

  TimeSeriesThresholdComparisonType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @JsonCreator
  public static TimeSeriesThresholdComparisonType fromDisplayName(String displayName) {
    for (TimeSeriesThresholdComparisonType timeSeriesThresholdComparisonType :
        TimeSeriesThresholdComparisonType.values()) {
      if (timeSeriesThresholdComparisonType.displayName.equals(displayName)) {
        return timeSeriesThresholdComparisonType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + displayName);
  }
}
