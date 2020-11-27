package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.ExecutableProcessor;
import io.harness.engine.executables.ResumePackage;
import io.harness.execution.NodeExecution;
import io.harness.pms.execution.Status;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepResponse;
import io.harness.tasks.ResponseData;

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
  NodeExecution nodeExecution;
  OrchestrationEngine orchestrationEngine;
  ExecutableProcessor processor;

  @Override
  public void run() {
    try {
      if (asyncError) {
        ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) response.values().iterator().next();
        StepResponse stepResponse = StepResponse.builder()
                                        .status(Status.ERRORED)
                                        .failureInfo(FailureInfo.builder()
                                                         .failureTypes(errorNotifyResponseData.getFailureTypes())
                                                         .errorMessage(errorNotifyResponseData.getErrorMessage())
                                                         .build())
                                        .build();
        orchestrationEngine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
        return;
      }

      processor.handleResume(ResumePackage.builder().nodeExecution(nodeExecution).responseDataMap(response).build());
    } catch (Exception ex) {
      orchestrationEngine.handleError(nodeExecution.getAmbiance(), ex);
    }
  }
}
