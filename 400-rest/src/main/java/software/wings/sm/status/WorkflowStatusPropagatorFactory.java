package software.wings.sm.status;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;

import software.wings.sm.status.handlers.NoopWorkflowPropagator;
import software.wings.sm.status.handlers.WorkflowPausePropagator;
import software.wings.sm.status.handlers.WorkflowResumePropagator;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class WorkflowStatusPropagatorFactory {
  @Inject private WorkflowPausePropagator workflowPausePropagator;
  @Inject private NoopWorkflowPropagator noopWorkflowPropagator;
  @Inject private WorkflowResumePropagator workflowResumePropagator;

  public WorkflowStatusPropagator obtainHandler(ExecutionStatus status) {
    switch (status) {
      case PAUSED:
        return workflowPausePropagator;
      case RESUMED:
        return workflowResumePropagator;
      default:
        return noopWorkflowPropagator;
    }
  }
}
