package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.execution.status.Status.brokeStatuses;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.OrchestrationEngine;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.chain.child.ChildChainExecutable;
import io.harness.facilitator.modes.chain.child.ChildChainResponse;
import io.harness.facilitator.modes.chain.task.TaskChainExecutable;
import io.harness.facilitator.modes.chain.task.TaskChainExecutableResponse;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.facilitator.modes.task.TaskExecutable;
import io.harness.plan.PlanNode;
import io.harness.registries.state.StepRegistry;
import io.harness.state.Step;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponseNotifyData;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

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
  Ambiance ambiance;
  OrchestrationEngine orchestrationEngine;
  Injector injector;
  StepRegistry stepRegistry;

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

      PlanNode node = nodeExecution.getNode();
      StepResponse stepResponse = null;
      Step step = stepRegistry.obtain(node.getStepType());
      switch (nodeExecution.getMode()) {
        case CHILDREN:
          ChildrenExecutable childrenExecutable = (ChildrenExecutable) step;
          stepResponse =
              childrenExecutable.handleChildrenResponse(ambiance, nodeExecution.getResolvedStepParameters(), response);
          break;
        case ASYNC:
          AsyncExecutable asyncExecutable = (AsyncExecutable) step;
          stepResponse =
              asyncExecutable.handleAsyncResponse(ambiance, nodeExecution.getResolvedStepParameters(), response);
          break;
        case CHILD:
          ChildExecutable childExecutable = (ChildExecutable) step;
          stepResponse =
              childExecutable.handleChildResponse(ambiance, nodeExecution.getResolvedStepParameters(), response);
          break;
        case TASK:
        case TASK_V2:
          TaskExecutable taskExecutable = (TaskExecutable) step;
          stepResponse = taskExecutable.handleTaskResult(ambiance, nodeExecution.getResolvedStepParameters(), response);
          break;
        case TASK_CHAIN:
        case TASK_CHAIN_V2:
          TaskChainExecutable taskChainExecutable = (TaskChainExecutable) step;
          TaskChainExecutableResponse lastLinkResponse =
              Preconditions.checkNotNull((TaskChainExecutableResponse) nodeExecution.obtainLatestExecutableResponse());
          if (lastLinkResponse.isChainEnd()) {
            stepResponse = taskChainExecutable.finalizeExecution(
                ambiance, nodeExecution.getResolvedStepParameters(), lastLinkResponse.getPassThroughData(), response);
            break;
          } else {
            orchestrationEngine.triggerLink(
                step, ambiance, nodeExecution, lastLinkResponse.getPassThroughData(), response);
            return;
          }
        case CHILD_CHAIN:
          ChildChainExecutable childChainExecutable = (ChildChainExecutable) stepRegistry.obtain(node.getStepType());
          ChildChainResponse lastChildChainExecutableResponse =
              Preconditions.checkNotNull((ChildChainResponse) nodeExecution.obtainLatestExecutableResponse());
          StepResponseNotifyData responseNotifyData = (StepResponseNotifyData) response.values().iterator().next();
          if (lastChildChainExecutableResponse.isLastLink() || brokeStatuses().contains(responseNotifyData.getStatus())
              || lastChildChainExecutableResponse.isSuspend()) {
            stepResponse = childChainExecutable.finalizeExecution(ambiance, nodeExecution.getResolvedStepParameters(),
                lastChildChainExecutableResponse.getPassThroughData(), response);
            break;
          } else {
            orchestrationEngine.triggerLink(
                step, ambiance, nodeExecution, lastChildChainExecutableResponse.getPassThroughData(), response);
            return;
          }
        default:
          throw new InvalidRequestException("Resume not handled for execution Mode : " + nodeExecution.getMode());
      }
      Preconditions.checkNotNull(
          stepResponse, "Step Response Cannot Be null. NodeExecutionId: " + nodeExecution.getUuid());
      orchestrationEngine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
    } catch (Exception ex) {
      orchestrationEngine.handleError(ambiance, ex);
    }
  }
}