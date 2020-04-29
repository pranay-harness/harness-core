package io.harness.engine.resume;

import com.google.inject.Injector;

import io.harness.annotations.Redesign;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.ExecutionEngine;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitate.modes.async.AsyncExecutable;
import io.harness.facilitate.modes.child.ChildExecutable;
import io.harness.facilitate.modes.children.ChildrenExecutable;
import io.harness.plan.ExecutionNode;
import io.harness.registries.state.StateRegistry;
import io.harness.state.execution.NodeExecution;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateResponse.FailureInfo;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Value
@Builder
@Slf4j
@Redesign
public class EngineResumeExecutor implements Runnable {
  boolean asyncError;
  Map<String, ResponseData> response;
  NodeExecution nodeExecution;
  ExecutionEngine executionEngine;
  Injector injector;
  StateRegistry stateRegistry;

  @Override
  public void run() {
    try {
      if (asyncError) {
        ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) response.values().iterator().next();
        StateResponse stateResponse = StateResponse.builder()
                                          .status(NodeExecutionStatus.ERRORED)
                                          .failureInfo(FailureInfo.builder()
                                                           .failureTypes(errorNotifyResponseData.getFailureTypes())
                                                           .errorMessage(errorNotifyResponseData.getErrorMessage())
                                                           .build())
                                          .build();
        executionEngine.handleStateResponse(nodeExecution.getUuid(), stateResponse);
        return;
      }

      ExecutionNode node = nodeExecution.getNode();
      switch (nodeExecution.getMode()) {
        case CHILDREN:
          resumeChildrenExecutable(node);
          break;
        case ASYNC:
          resumeAsyncExecutable(node);
          break;
        case CHILD:
          resumeChildExecutable(node);
          break;
        default:
          throw new InvalidRequestException("Resume not handled for execution Mode : " + nodeExecution.getMode());
      }

    } catch (Exception ex) {
      logger.error(ex.getMessage());
    }
  }

  private void resumeAsyncExecutable(ExecutionNode node) {
    AsyncExecutable asyncExecutable = (AsyncExecutable) stateRegistry.obtain(node.getStateType());
    StateResponse stateResponse =
        asyncExecutable.handleAsyncResponse(nodeExecution.getAmbiance(), node.getStateParameters(), response);
    executionEngine.handleStateResponse(nodeExecution.getUuid(), stateResponse);
  }

  private void resumeChildrenExecutable(ExecutionNode node) {
    ChildrenExecutable childrenExecutable = (ChildrenExecutable) stateRegistry.obtain(node.getStateType());
    StateResponse stateResponse =
        childrenExecutable.handleAsyncResponse(nodeExecution.getAmbiance(), node.getStateParameters(), response);
    executionEngine.handleStateResponse(nodeExecution.getUuid(), stateResponse);
  }

  private void resumeChildExecutable(ExecutionNode node) {
    ChildExecutable childExecutable = (ChildExecutable) stateRegistry.obtain(node.getStateType());
    StateResponse stateResponse =
        childExecutable.handleAsyncResponse(nodeExecution.getAmbiance(), node.getStateParameters(), response);
    executionEngine.handleStateResponse(nodeExecution.getUuid(), stateResponse);
  }
}