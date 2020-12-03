package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.AmbianceUtils;
import io.harness.OrchestrationPublisherName;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.ExecuteStrategy;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.ResumePackage;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.AsyncExecutableResponse;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.execution.Status;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.registries.state.StepRegistry;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
@Slf4j
@Redesign
public class AsyncStrategy implements ExecuteStrategy {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private OrchestrationEngine engine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecution nodeExecution = invokerPackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    AsyncExecutable asyncExecutable = extractAsyncExecutable(invokerPackage.getNodeExecution());
    AsyncExecutableResponse asyncExecutableResponse = asyncExecutable.executeAsync(
        ambiance, nodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(ambiance, asyncExecutableResponse);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecution nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    AsyncExecutable asyncExecutable = extractAsyncExecutable(nodeExecution);
    StepResponse stepResponse = asyncExecutable.handleAsyncResponse(ambiance,
        nodeExecutionService.extractResolvedStepParameters(nodeExecution), resumePackage.getResponseDataMap());
    engine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
  }

  private void handleResponse(Ambiance ambiance, AsyncExecutableResponse response) {
    NodeExecution nodeExecution =
        Preconditions.checkNotNull(nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance)));
    PlanNodeProto node = nodeExecution.getNode();
    if (isEmpty(response.getCallbackIdsList())) {
      log.error("StepResponse has no callbackIds - currentState : " + node.getName()
          + ", nodeExecutionId: " + nodeExecution.getUuid());
      throw new InvalidRequestException("Callback Ids cannot be empty for Async Executable Response");
    }
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, response.getCallbackIdsList().toArray(new String[0]));
    // Update Execution Node Instance state to TASK_WAITING
    nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), Status.ASYNC_WAITING,
        ops
        -> ops.addToSet(
            NodeExecutionKeys.executableResponses, ExecutableResponse.newBuilder().setAsync(response).build()));
  }

  private AsyncExecutable extractAsyncExecutable(NodeExecution nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (AsyncExecutable) stepRegistry.obtain(node.getStepType());
  }
}
