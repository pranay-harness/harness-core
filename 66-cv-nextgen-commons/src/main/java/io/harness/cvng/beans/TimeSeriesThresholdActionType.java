package io.harness.cvng.beans;

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
}
