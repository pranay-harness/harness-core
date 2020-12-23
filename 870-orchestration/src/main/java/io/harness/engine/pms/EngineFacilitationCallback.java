package io.harness.engine.pms;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.FailureResponseData;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyCallback;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;
import lombok.SneakyThrows;

@OwnedBy(CDC)
public class EngineFacilitationCallback implements NotifyCallback {
  @Inject private OrchestrationEngine orchestrationEngine;

  String nodeExecutionId;

  @Builder
  EngineFacilitationCallback(String nodeExecutionId) {
    this.nodeExecutionId = nodeExecutionId;
  }

  @SneakyThrows
  @Override
  public void notify(Map<String, ResponseData> response) {
    BinaryResponseData binaryResponseData = (BinaryResponseData) response.values().iterator().next();
    FacilitatorResponseProto facilitatorResponseProto =
        FacilitatorResponseProto.parseFrom(binaryResponseData.getData());
    if (facilitatorResponseProto.getIsSuccessful()) {
      orchestrationEngine.facilitateExecution(nodeExecutionId, facilitatorResponseProto);
    } else {
      // TODO => Handle Error
    }
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    FailureResponseData failureResponseData = (FailureResponseData) response.values().iterator().next();
    StepResponseProto stepResponseProto =
        StepResponseProto.newBuilder()
            .setStatus(Status.FAILED)
            .setFailureInfo(FailureInfo.newBuilder()
                                .setErrorMessage(failureResponseData.getErrorMessage())
                                .addAllFailureTypes(EngineExceptionUtils.transformToOrchestrationFailureTypes(
                                    failureResponseData.getFailureTypes()))
                                .build())
            .build();
    orchestrationEngine.handleStepResponse(nodeExecutionId, stepResponseProto);
  }
}
