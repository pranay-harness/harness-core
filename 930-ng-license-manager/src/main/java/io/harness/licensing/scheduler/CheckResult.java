package io.harness.licensing.scheduler;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckResult {
  private boolean allInactive;
  private boolean isUpdated;
  private long maxExpiryTime;
  private int totalWorkload;
  private long totalClientMAU;
  private int totalFeatureFlagUnit;
  private int totalDevelopers;

  public CheckResult combine(CheckResult other) {
    allInactive = allInactive && other.allInactive;
    isUpdated = isUpdated || other.isUpdated;
    return this;
  }

  public static CheckResult newDefaultResult() {
    return CheckResult.builder().allInactive(true).isUpdated(false).build();
  }
}
