package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.pms.start.NodeStartHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;

@OwnedBy(CDC)
public class EngineWaitResumeCallback implements OldNotifyCallback {
  @Inject private NodeStartHelper nodeStartHelper;

  Ambiance ambiance;
  FacilitatorResponseProto facilitatorResponse;

  @Builder
  EngineWaitResumeCallback(
      Ambiance ambiance, FacilitatorResponseProto facilitatorResponse, StepInputPackage inputPackage) {
    this.ambiance = ambiance;
    this.facilitatorResponse = facilitatorResponse;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    nodeStartHelper.startNode(ambiance, facilitatorResponse);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    // TODO => Handle Error
  }
}
