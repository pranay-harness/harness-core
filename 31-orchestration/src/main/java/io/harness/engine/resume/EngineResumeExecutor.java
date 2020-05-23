package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Injector;

import io.harness.annotations.Redesign;
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
import io.harness.facilitator.modes.task.AsyncTaskExecutable;
import io.harness.plan.ExecutionNode;
import io.harness.registries.state.StepRegistry;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.FailureInfo;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@OwnedBy(CDC)
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
  StepRegistry stepRegistry;

  @Override
  public void run() {
    try {
      if (asyncError) {
        ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) response.values().iterator().next();
        StepResponse stepResponse = StepResponse.builder()
                                        .status(NodeExecutionStatus.ERRORED)
                                        .failureInfo(FailureInfo.builder()
                                                         .failureTypes(errorNotifyResponseData.getFailureTypes())
                                                         .errorMessage(errorNotifyResponseData.getErrorMessage())
                                                         .build())
                                        .build();
        executionEngine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
        return;
      }

      ExecutionNode node = nodeExecution.getNode();
      StepResponse stepResponse = null;
      switch (nodeExecution.getMode()) {
        case CHILDREN:
          ChildrenExecutable childrenExecutable = (ChildrenExecutable) stepRegistry.obtain(node.getStepType());
          stepResponse = childrenExecutable.handleChildrenResponse(
              nodeExecution.getAmbiance(), nodeExecution.getResolvedStepParameters(), response);
          break;
        case ASYNC:
          AsyncExecutable asyncExecutable = (AsyncExecutable) stepRegistry.obtain(node.getStepType());
          stepResponse = asyncExecutable.handleAsyncResponse(
              nodeExecution.getAmbiance(), nodeExecution.getResolvedStepParameters(), response);
          break;
        case CHILD:
          ChildExecutable childExecutable = (ChildExecutable) stepRegistry.obtain(node.getStepType());
          stepResponse = childExecutable.handleChildResponse(
              nodeExecution.getAmbiance(), nodeExecution.getResolvedStepParameters(), response);
          break;
        case ASYNC_TASK:
          AsyncTaskExecutable asyncTaskExecutable = (AsyncTaskExecutable) stepRegistry.obtain(node.getStepType());
          stepResponse = asyncTaskExecutable.handleTaskResult(
              nodeExecution.getAmbiance(), nodeExecution.getResolvedStepParameters(), response);
          break;
        default:
          throw new InvalidRequestException("Resume not handled for execution Mode : " + nodeExecution.getMode());
      }
      executionEngine.handleStepResponse(nodeExecution.getUuid(), stepResponse);

    } catch (Exception ex) {
      logger.error(ex.getMessage());
    }
  }
}