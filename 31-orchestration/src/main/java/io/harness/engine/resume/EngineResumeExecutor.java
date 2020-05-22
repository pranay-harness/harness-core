package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Injector;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.ExecutionEngine;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.facilitator.modes.task.TaskWrapperExecutable;
import io.harness.plan.ExecutionNode;
import io.harness.registries.state.StateRegistry;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateResponse.FailureInfo;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
@Slf4j
@Redesign
@ExcludeRedesign
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
      StateResponse stateResponse = null;
      switch (nodeExecution.getMode()) {
        case CHILDREN:
          ChildrenExecutable childrenExecutable = (ChildrenExecutable) stateRegistry.obtain(node.getStateType());
          stateResponse = childrenExecutable.handleChildrenResponse(
              nodeExecution.getAmbiance(), node.getStateParameters(), response);
          break;
        case ASYNC:
          AsyncExecutable asyncExecutable = (AsyncExecutable) stateRegistry.obtain(node.getStateType());
          stateResponse =
              asyncExecutable.handleAsyncResponse(nodeExecution.getAmbiance(), node.getStateParameters(), response);
          break;
        case CHILD:
          ChildExecutable childExecutable = (ChildExecutable) stateRegistry.obtain(node.getStateType());
          stateResponse =
              childExecutable.handleChildResponse(nodeExecution.getAmbiance(), node.getStateParameters(), response);
          break;
        case TASK_WRAPPER:
          TaskWrapperExecutable taskWrapperExecutable =
              (TaskWrapperExecutable) stateRegistry.obtain(node.getStateType());
          stateResponse =
              taskWrapperExecutable.handleTaskResult(nodeExecution.getAmbiance(), node.getStateParameters(), response);
          break;
        default:
          throw new InvalidRequestException("Resume not handled for execution Mode : " + nodeExecution.getMode());
      }
      executionEngine.handleStateResponse(nodeExecution.getUuid(), stateResponse);

    } catch (Exception ex) {
      logger.error(ex.getMessage());
    }
  }
}