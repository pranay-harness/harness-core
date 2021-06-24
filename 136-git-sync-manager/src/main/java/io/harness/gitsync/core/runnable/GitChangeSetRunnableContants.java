package io.harness.gitsync.core.runnable;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public class GitChangeSetRunnableContants {
  public static final int MAX_RUNNING_CHANGESETS_FOR_ACCOUNT = 5;
  public static final int MAX_RETRY_FOR_CHANGESET = 10;
  public static final int STATUS_LOG_PRINT_INTERVAL = 1 /*min*/;
  public static final int STUCK_RUNNING_JOB_CHECK_INTERVAL = 30 /*min*/;
  public static final int MAX_RETRIED_QUEUED_JOB_CHECK_INTERVAL = 5 /*min*/;
}
