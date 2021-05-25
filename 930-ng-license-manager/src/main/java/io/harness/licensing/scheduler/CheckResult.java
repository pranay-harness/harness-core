package io.harness.licensing.scheduler;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckResult {
  private boolean allInactive;
  private boolean isUpdated;

  public CheckResult combine(CheckResult other) {
    allInactive = allInactive && other.allInactive;
    isUpdated = isUpdated || other.isUpdated;
    return this;
  }
}
