package io.harness.cvng.servicelevelobjective.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SLIValue {
  int goodCount;
  int badCount;
  int total;
  public double sliPercentage() {
    if (total == 0) {
      return 100.0;
    } else {
      return (goodCount * 100.0) / total;
    }
  }
}
