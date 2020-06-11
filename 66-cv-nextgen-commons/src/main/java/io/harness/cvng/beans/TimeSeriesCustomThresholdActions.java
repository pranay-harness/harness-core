package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TimeSeriesCustomThresholdActions {
  FAIL_IMMEDIATELY("fail-immediately"),
  FAIL_AFTER_OCCURRENCES("fail-after-multiple-occurrences"),
  FAIL_AFTER_CONSECUTIVE_OCCURRENCES("fail-after-consecutive-occurrences");

  private String displayName;

  TimeSeriesCustomThresholdActions(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }
}
