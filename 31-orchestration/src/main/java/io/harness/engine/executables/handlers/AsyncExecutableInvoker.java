package io.harness.engine.executables.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.engine.services.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.plan.PlanNode;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Redesign
public class AsyncExecutableInvoker implements ExecutableInvoker {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private AmbianceHelper ambianceHelper;

  @Override
  public void invokeExecutable(InvokerPackage invokerPackage) {
    AsyncExecutable asyncExecutable = (AsyncExecutable) invokerPackage.getStep();
    Ambiance ambiance = invokerPackage.getAmbiance();
    AsyncExecutableResponse asyncExecutableResponse =
        asyncExecutable.executeAsync(ambiance, invokerPackage.getParameters(), invokerPackage.getInputs());
    handleResponse(ambiance, asyncExecutableResponse);
  }

  private void handleResponse(Ambiance ambiance, AsyncExecutableResponse response) {
    NodeExecution nodeExecution = Preconditions.checkNotNull(ambianceHelper.obtainNodeExecution(ambiance));
    PlanNode node = nodeExecution.getNode();
    if (isEmpty(response.getCallbackIds())) {
      logger.error("StepResponse has no callbackIds - currentState : " + node.getName()
          + ", nodeExecutionId: " + nodeExecution.getUuid());
      throw new InvalidRequestException("Callback Ids cannot be empty for Async Executable Response");
    }
    NotifyCallback callback = EngineResumeCallback.builder().nodeInstanceId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, response.getCallbackIds().toArray(new String[0]));

    // Update Execution Node Instance state to TASK_WAITING
    nodeExecutionService.update(
        nodeExecution.getUuid(), ops -> ops.addToSet(NodeExecutionKeys.executableResponses, response));
  }
}
