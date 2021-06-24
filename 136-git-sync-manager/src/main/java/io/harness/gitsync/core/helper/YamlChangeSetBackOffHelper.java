package io.harness.gitsync.core.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class YamlChangeSetBackOffHelper {
  private static final long MAX_BACKOFF = 1800000; /* 30 mins */
  private static final long MIN_BACKOFF = 180000; /* 3 mins */
  private static final int[] FIBONACCI = new int[] {1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144};

  public long getCutOffTime(long retryCount, long currentTime) {
    final double backoff = Math.pow(2, retryCount) + MIN_BACKOFF;
    return (long) (currentTime + Math.min(backoff, MAX_BACKOFF));
  }

  public long getNextRunTime(int retryCount, long currentTime) {
    if (retryCount > FIBONACCI.length) {
      retryCount = FIBONACCI.length - 1;
    }
    final long backoff = FIBONACCI[retryCount] * 100000;
    return currentTime + backoff;
  }
}
