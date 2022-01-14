package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

/**
 * Created by rishi on 12/23/16.
 */
@OwnedBy(CDC)
public enum ExecutionScope {
  WORKFLOW,
  WORKFLOW_PHASE;
}
