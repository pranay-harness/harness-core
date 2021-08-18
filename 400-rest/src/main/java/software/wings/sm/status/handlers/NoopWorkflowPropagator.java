package software.wings.sm.status.handlers;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.status.StateStatusUpdateInfo;
import software.wings.sm.status.WorkflowStatusPropagator;

import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class NoopWorkflowPropagator implements WorkflowStatusPropagator {
  public void handleStatusUpdate(@NotNull StateStatusUpdateInfo stateStatusUpdateInfo) {
    // Do Nothing
  }
}
