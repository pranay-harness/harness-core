package io.harness.engine.executables.handlers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.execution.status.NodeExecutionStatus.TASK_WAITING;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.EngineStatusHelper;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.plan.ExecutionNode;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Redesign
public class AsyncExecutableInvoker implements ExecutableInvoker {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private EngineStatusHelper engineStatusHelper;
  @Inject private AmbianceHelper ambianceHelper;

  @Override
  public void invokeExecutable(InvokerPackage invokerPackage) {
    AsyncExecutable asyncExecutable = (AsyncExecutable) invokerPackage.getState();
    Ambiance ambiance = invokerPackage.getAmbiance();
    AsyncExecutableResponse asyncExecutableResponse =
        asyncExecutable.executeAsync(ambiance, invokerPackage.getParameters(), invokerPackage.getInputs());
    handleResponse(ambiance, asyncExecutableResponse);
  }

  private void handleResponse(Ambiance ambiance, AsyncExecutableResponse response) {
    NodeExecution nodeExecution = Preconditions.checkNotNull(ambianceHelper.obtainNodeExecution(ambiance));
    ExecutionNode nodeDefinition = nodeExecution.getNode();
    if (isEmpty(response.getCallbackIds())) {
      logger.error("StateResponse has no callbackIds - currentState : " + nodeDefinition.getName()
          + ", nodeExecutionId: " + nodeExecution.getUuid());
      throw new InvalidRequestException("Callback Ids cannot be empty for Async Executable Response");
    }
    NotifyCallback callback = EngineResumeCallback.builder().nodeInstanceId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, response.getCallbackIds().toArray(new String[0]));

    // Update Execution Node Instance state to TASK_WAITING
    engineStatusHelper.updateNodeInstance(
        nodeExecution.getUuid(), ops -> ops.set(NodeExecutionKeys.status, TASK_WAITING));
  }
}
