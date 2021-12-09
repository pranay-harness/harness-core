package io.harness.cvng.servicelevelobjective.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SLIMissingDataType {
  @JsonProperty("Good") GOOD,
  @JsonProperty("Bad") BAD,
  @JsonProperty("Ignore") IGNORE;

  public double calculateSLIValue(long goodCount, long badCount, long totalMinutes) {
    long missingDataCount = totalMinutes - (goodCount + badCount);
    switch (this) {
      case GOOD:
        return ((goodCount + missingDataCount) * 100.0) / totalMinutes;
      case BAD:
        return (goodCount * 100.0) / totalMinutes;
      case IGNORE:
        return (goodCount * 100.0) / (goodCount + badCount);
      default:
        throw new IllegalStateException("Unhanded SLIMissingDataType " + this);
    }
  }
}
