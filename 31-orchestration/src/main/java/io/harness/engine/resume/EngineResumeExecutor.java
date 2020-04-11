package io.harness.engine.resume;

import com.google.inject.Injector;

import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.ExecutionEngine;
import io.harness.facilitate.modes.async.AsyncExecutable;
import io.harness.plan.ExecutionNode;
import io.harness.registries.state.StateRegistry;
import io.harness.state.execution.ExecutionNodeInstance;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.StateResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Value
@Builder
@Slf4j
public class EngineResumeExecutor implements Runnable {
  boolean asyncError;
  Map<String, ResponseData> response;
  ExecutionNodeInstance executionNodeInstance;
  ExecutionEngine executionEngine;
  Injector injector;
  StateRegistry stateRegistry;

  @Override
  public void run() {
    try {
      if (asyncError) {
        ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) response.values().iterator().next();
        StateResponse stateResponse = StateResponse.builder()
                                          .executionStatus(NodeExecutionStatus.ERRORED)
                                          .errorMessage(errorNotifyResponseData.getErrorMessage())
                                          .build();
        executionEngine.handleStateResponse(executionNodeInstance.getUuid(), stateResponse);
        return;
      }

      ExecutionNode nodeDefinition = executionNodeInstance.getNode();
      AsyncExecutable asyncExecutable = (AsyncExecutable) stateRegistry.obtain(nodeDefinition.getStateType());
      StateResponse stateResponse = asyncExecutable.handleAsyncResponse(executionNodeInstance.getAmbiance(), response);
      executionEngine.handleStateResponse(executionNodeInstance.getUuid(), stateResponse);
    } catch (Exception ex) {
      logger.error(ex.getMessage());
    }
  }
}
