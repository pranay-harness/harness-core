package software.wings.helpers.ext.gcb.models;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum GcbBuildStatus {
  STATUS_UNKNOWN,
  QUEUED,
  WORKING,
  SUCCESS,
  FAILURE,
  INTERNAL_ERROR,
  TIMEOUT,
  CANCELLED,
  EXPIRED
}
