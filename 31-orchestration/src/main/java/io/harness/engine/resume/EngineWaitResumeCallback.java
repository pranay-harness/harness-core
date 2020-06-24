package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.ExecutionEngine;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.persistence.converters.DurationConverter;
import io.harness.state.io.StepInputPackage;
import io.harness.waiter.NotifyCallback;
import lombok.Builder;
import org.mongodb.morphia.annotations.Converters;

import java.util.Map;

@OwnedBy(CDC)
@Converters({DurationConverter.class})
public class EngineWaitResumeCallback implements NotifyCallback {
  @Inject private ExecutionEngine executionEngine;

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
    executionEngine.invokeState(ambiance, facilitatorResponse, inputPackage);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    // TODO => Handle Error
  }
}
