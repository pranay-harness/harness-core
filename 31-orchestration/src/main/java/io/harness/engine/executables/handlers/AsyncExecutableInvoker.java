package io.harness.engine.executables.handlers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.state.execution.status.NodeExecutionStatus.TASK_WAITING;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.EngineStatusHelper;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitate.modes.async.AsyncExecutable;
import io.harness.facilitate.modes.async.AsyncExecutableResponse;
import io.harness.plan.ExecutionNode;
import io.harness.state.execution.ExecutionNodeInstance;
import io.harness.state.io.ambiance.Ambiance;
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
    ExecutionNodeInstance nodeInstance = ambianceHelper.obtainNodeInstance(ambiance);
    ExecutionNode nodeDefinition = nodeInstance.getNode();
    if (isEmpty(response.getCallbackIds())) {
      logger.error("executionResponse is null, but no correlationId - currentState : " + nodeDefinition.getName()
          + ", stateExecutionInstanceId: " + nodeInstance.getUuid());
      throw new InvalidRequestException("Callback Ids cannot be empty for Async Executable Response");
    }
    NotifyCallback callback = EngineResumeCallback.builder().nodeInstanceId(nodeInstance.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, response.getCallbackIds().toArray(new String[0]));

    // Update Execution Node Instance state to TASK_WAITING
    engineStatusHelper.updateNodeInstance(nodeInstance.getUuid(), TASK_WAITING, operations -> {});
  }
}
