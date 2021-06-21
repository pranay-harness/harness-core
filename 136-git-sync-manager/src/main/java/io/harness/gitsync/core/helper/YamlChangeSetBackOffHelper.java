package io.harness.gitsync.core.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class YamlChangeSetBackOffHelper {
  private static final long MAX_BACKOFF = 1800000; /* 30 mins */
  private static final long MIN_BACKOFF = 180000; /* 3 mins */

  public long getCutOffTime(long retryCount, long currentTime) {
    final double backoff = Math.pow(2, retryCount) + MIN_BACKOFF;
    return (long) (currentTime + Math.min(backoff, MAX_BACKOFF));
  }
}
