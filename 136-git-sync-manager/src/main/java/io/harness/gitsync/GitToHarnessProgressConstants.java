package io.harness.gitsync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public class GitToHarnessProgressConstants {
  public static final long longRunningEventResetDurationInMs = 5 * 60 * 1000;
}
