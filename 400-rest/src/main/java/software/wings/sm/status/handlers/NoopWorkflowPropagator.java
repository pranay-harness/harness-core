package software.wings.sm.status.handlers;

import software.wings.sm.status.StateStatusUpdateInfo;
import software.wings.sm.status.WorkflowStatusPropagator;

import javax.validation.constraints.NotNull;

public class NoopWorkflowPropagator implements WorkflowStatusPropagator {
  public void handleStatusUpdate(@NotNull StateStatusUpdateInfo stateStatusUpdateInfo) {
    // Do Nothing
  }
}
