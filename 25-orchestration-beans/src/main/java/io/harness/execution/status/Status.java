package io.harness.execution.status;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

import java.util.EnumSet;

@OwnedBy(CDC)
@Redesign
public enum Status {
  // In Progress statuses : All the in progress statuses named with ing in the end
  RUNNING,
  WAITING,

  DISCONTINUING,
  PAUSING,

  // Final Statuses : All the final statuses named with ed in the end
  QUEUED,
  SKIPPED,
  PAUSED,
  ABORTED,
  ERRORED,
  FAILED,
  SUCCEEDED;

  // Status Groups
  private static final EnumSet<Status> ABORTABLE_STATUSES = EnumSet.of(QUEUED, RUNNING, PAUSED, PAUSING, WAITING);

  private static final EnumSet<Status> POSITIVE_STATUSES = EnumSet.of(SUCCEEDED, SKIPPED);

  private static final EnumSet<Status> BROKE_STATUSES = EnumSet.of(FAILED, ERRORED);

  private static final EnumSet<Status> RESUMABLE_STATUSES = EnumSet.of(QUEUED, RUNNING, WAITING);

  private static final EnumSet<Status> FLOWING_STATUSES = EnumSet.of(RUNNING, DISCONTINUING);

  public static EnumSet<Status> abortableStatuses() {
    return ABORTABLE_STATUSES;
  }

  public static EnumSet<Status> positiveStatuses() {
    return POSITIVE_STATUSES;
  }

  public static EnumSet<Status> brokeStatuses() {
    return BROKE_STATUSES;
  }

  public static EnumSet<Status> resumableStatuses() {
    return RESUMABLE_STATUSES;
  }

  public static EnumSet<Status> flowingStatuses() {
    return FLOWING_STATUSES;
  }
}
