package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.ExecutionEngine;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.chain.TaskChainExecutable;
import io.harness.facilitator.modes.chain.TaskChainExecutableResponse;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.facilitator.modes.task.TaskExecutable;
import io.harness.plan.PlanNode;
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
  Ambiance ambiance;
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

      PlanNode node = nodeExecution.getNode();
      StepResponse stepResponse = null;
      switch (nodeExecution.getMode()) {
        case CHILDREN:
          ChildrenExecutable childrenExecutable = (ChildrenExecutable) stepRegistry.obtain(node.getStepType());
          stepResponse =
              childrenExecutable.handleChildrenResponse(ambiance, nodeExecution.getResolvedStepParameters(), response);
          break;
        case ASYNC:
          AsyncExecutable asyncExecutable = (AsyncExecutable) stepRegistry.obtain(node.getStepType());
          stepResponse =
              asyncExecutable.handleAsyncResponse(ambiance, nodeExecution.getResolvedStepParameters(), response);
          break;
        case CHILD:
          ChildExecutable childExecutable = (ChildExecutable) stepRegistry.obtain(node.getStepType());
          stepResponse =
              childExecutable.handleChildResponse(ambiance, nodeExecution.getResolvedStepParameters(), response);
          break;
        case TASK:
          TaskExecutable taskExecutable = (TaskExecutable) stepRegistry.obtain(node.getStepType());
          stepResponse = taskExecutable.handleTaskResult(ambiance, nodeExecution.getResolvedStepParameters(), response);
          break;
        case TASK_CHAIN:
          TaskChainExecutable taskChainExecutable = (TaskChainExecutable) stepRegistry.obtain(node.getStepType());
          TaskChainExecutableResponse lastLinkResponse =
              (TaskChainExecutableResponse) nodeExecution.getExecutableResponse();
          if (lastLinkResponse.isChainEnd()) {
            stepResponse =
                taskChainExecutable.finalizeExecution(ambiance, nodeExecution.getResolvedStepParameters(), response);
            break;
          } else {
            executionEngine.triggerLink(taskChainExecutable, ambiance, nodeExecution, response);
            return;
          }
        default:
          throw new InvalidRequestException("Resume not handled for execution Mode : " + nodeExecution.getMode());
      }
      Preconditions.checkNotNull(
          stepResponse, "Step Response Cannot Be null. NodeExecutionId: " + nodeExecution.getUuid());
      executionEngine.handleStepResponse(nodeExecution.getUuid(), stepResponse);

    } catch (Exception ex) {
      logger.error(ex.getMessage());
    }
  }
}