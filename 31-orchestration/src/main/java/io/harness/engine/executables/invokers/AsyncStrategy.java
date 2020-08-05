package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.OrchestrationPublisherName;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executables.InvokeStrategy;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.plan.PlanNode;
import io.harness.state.io.StepParameters;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Redesign
public class AsyncStrategy implements InvokeStrategy {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  @Override
  public void invoke(InvokerPackage invokerPackage) {
    AsyncExecutable<StepParameters> asyncExecutable = (AsyncExecutable<StepParameters>) invokerPackage.getStep();
    Ambiance ambiance = invokerPackage.getAmbiance();
    AsyncExecutableResponse asyncExecutableResponse =
        asyncExecutable.executeAsync(ambiance, invokerPackage.getParameters(), invokerPackage.getInputPackage());
    handleResponse(ambiance, asyncExecutableResponse);
  }

  private void handleResponse(Ambiance ambiance, AsyncExecutableResponse response) {
    NodeExecution nodeExecution =
        Preconditions.checkNotNull(nodeExecutionService.get(ambiance.obtainCurrentRuntimeId()));
    PlanNode node = nodeExecution.getNode();
    if (isEmpty(response.getCallbackIds())) {
      logger.error("StepResponse has no callbackIds - currentState : " + node.getName()
          + ", nodeExecutionId: " + nodeExecution.getUuid());
      throw new InvalidRequestException("Callback Ids cannot be empty for Async Executable Response");
    }
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, response.getCallbackIds().toArray(new String[0]));

    // Update Execution Node Instance state to TASK_WAITING
    nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), Status.ASYNC_WAITING,
        ops -> ops.addToSet(NodeExecutionKeys.executableResponses, response));
  }
}
