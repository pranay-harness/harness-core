package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.engine.EngineExceptionUtils;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.ExecutableProcessor;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
@Value
@Builder
@Slf4j
@Redesign
public class EngineResumeExecutor implements Runnable {
  boolean asyncError;
  Map<String, ResponseData> response;
  NodeExecutionProto nodeExecution;
  List<PlanNodeProto> nodes;
  OrchestrationEngine orchestrationEngine;
  ExecutableProcessor processor;

  @Override
  public void run() {
    try {
      if (asyncError) {
        ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) response.values().iterator().next();
        StepResponse stepResponse = StepResponse.builder()
                                        .status(Status.ERRORED)
                                        .failureInfo(FailureInfo.newBuilder()
                                                         .addAllFailureTypes(EngineExceptionUtils.transformFailureTypes(
                                                             errorNotifyResponseData.getFailureTypes()))
                                                         .setErrorMessage(errorNotifyResponseData.getErrorMessage())
                                                         .build())
                                        .build();
        orchestrationEngine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
        return;
      }

      processor.handleResume(
          ResumePackage.builder().nodeExecution(nodeExecution).nodes(nodes).responseDataMap(response).build());
    } catch (Exception ex) {
      orchestrationEngine.handleError(nodeExecution.getAmbiance(), ex);
    }
  }
}
