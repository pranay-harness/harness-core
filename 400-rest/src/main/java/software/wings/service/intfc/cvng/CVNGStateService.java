package software.wings.service.intfc.cvng;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

import software.wings.sm.states.CVNGState.StepStatus;

@OwnedBy(CV)
public interface CVNGStateService {
  void notifyWorkflowCVNGState(String activityId, StepStatus status);
}
