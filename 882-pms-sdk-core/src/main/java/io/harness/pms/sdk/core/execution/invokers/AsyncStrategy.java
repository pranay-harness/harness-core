package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.execution.EngineResumeCallback;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;
import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;
import io.harness.waiter.NotifyCallback;

import com.google.inject.Inject;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
@Slf4j
public class AsyncStrategy implements ExecuteStrategy {
  @Inject private PmsNodeExecutionService pmsNodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private AsyncWaitEngine asyncWaitEngine;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    AsyncExecutable asyncExecutable = extractAsyncExecutable(nodeExecution);
    AsyncExecutableResponse asyncExecutableResponse = asyncExecutable.executeAsync(ambiance,
        pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(ambiance, nodeExecution, asyncExecutableResponse);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    AsyncExecutable asyncExecutable = extractAsyncExecutable(nodeExecution);
    StepResponse stepResponse = asyncExecutable.handleAsyncResponse(ambiance,
        pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), resumePackage.getResponseDataMap());
    pmsNodeExecutionService.handleStepResponse(
        nodeExecution.getUuid(), StepResponseMapper.toStepResponseProto(stepResponse));
  }

  private void handleResponse(Ambiance ambiance, NodeExecutionProto nodeExecution, AsyncExecutableResponse response) {
    PlanNodeProto node = nodeExecution.getNode();
    if (isEmpty(response.getCallbackIdsList())) {
      log.error("StepResponse has no callbackIds - currentState : " + node.getName()
          + ", nodeExecutionId: " + nodeExecution.getUuid());
      throw new InvalidRequestException("Callback Ids cannot be empty for Async Executable Response");
    }

    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    asyncWaitEngine.waitForAllOn(callback, response.getCallbackIdsList().toArray(new String[0]));
    pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.ASYNC_WAITING,
        ExecutableResponse.newBuilder().setAsync(response).build(), Collections.emptyList());
  }

  private AsyncExecutable extractAsyncExecutable(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (AsyncExecutable) stepRegistry.obtain(node.getStepType());
  }
}
