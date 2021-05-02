package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.utils.StatusUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(PIPELINE)
@Singleton
public class StepStatusUpdateFactory {
  @Inject ApprovalStepStatusUpdate approvalStepStatusUpdate;
  @Inject InterventionWaitStepStatusUpdate interventionWaitStepStatusUpdate;
  @Inject PausedStepStatusUpdate pausedStepStatusUpdate;
  @Inject ResumeStepStatusUpdate resumeStepStatusUpdate;
  @Inject TerminalStepStatusUpdate terminalStepStatusUpdate;
  @Inject AbortAndRunningStepStatusUpdate abortAndRunningStepStatusUpdate;

  public StepStatusUpdate obtainStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    switch (stepStatusUpdateInfo.getStatus()) {
      case APPROVAL_WAITING:
        return approvalStepStatusUpdate;
      case INTERVENTION_WAITING:
        return interventionWaitStepStatusUpdate;
      case PAUSED:
        return pausedStepStatusUpdate;
      case QUEUED:
        return resumeStepStatusUpdate;
      case RUNNING:
      case ABORTED:
        return abortAndRunningStepStatusUpdate;
      default:
        if (StatusUtils.isFinalStatus(stepStatusUpdateInfo.getStatus())) {
          return terminalStepStatusUpdate;
        }
        return null;
    }
  }
}
