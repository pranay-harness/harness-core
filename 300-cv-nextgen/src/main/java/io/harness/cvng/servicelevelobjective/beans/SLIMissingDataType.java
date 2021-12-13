package io.harness.cvng.servicelevelobjective.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

public enum SLIMissingDataType {
  @JsonProperty("Good") GOOD,
  @JsonProperty("Bad") BAD,
  @JsonProperty("Ignore") IGNORE;

  public SLIValue calculateSLIValue(long goodCount, long badCount, long totalMinutes) {
    Preconditions.checkState(totalMinutes != 0);
    long missingDataCount = totalMinutes - (goodCount + badCount);
    switch (this) {
      case GOOD:
        return SLIValue.builder()
            .goodCount((int) (goodCount + missingDataCount))
            .badCount((int) badCount)
            .total((int) totalMinutes)
            .build();
      case BAD:
        return SLIValue.builder()
            .goodCount((int) goodCount)
            .badCount((int) (badCount + missingDataCount))
            .total((int) totalMinutes)
            .build();
      case IGNORE:
        return SLIValue.builder()
            .goodCount((int) goodCount)
            .badCount((int) badCount)
            .total((int) (goodCount + badCount))
            .build();
      default:
        throw new IllegalStateException("Unhanded SLIMissingDataType " + this);
    }
  }
}
