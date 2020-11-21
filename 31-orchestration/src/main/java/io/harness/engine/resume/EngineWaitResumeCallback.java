package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.state.io.StepInputPackage;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyCallback;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;

@OwnedBy(CDC)
public class EngineWaitResumeCallback implements NotifyCallback {
  @Inject private OrchestrationEngine orchestrationEngine;

  Ambiance ambiance;
  FacilitatorResponse facilitatorResponse;
  StepInputPackage inputPackage;

  @Builder
  EngineWaitResumeCallback(Ambiance ambiance, FacilitatorResponse facilitatorResponse, StepInputPackage inputPackage) {
    this.ambiance = ambiance;
    this.facilitatorResponse = facilitatorResponse;
    this.inputPackage = inputPackage;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    orchestrationEngine.invokeExecutable(ambiance, facilitatorResponse, inputPackage);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    // TODO => Handle Error
  }
}
