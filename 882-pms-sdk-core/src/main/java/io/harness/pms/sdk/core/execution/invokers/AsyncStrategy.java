package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableMode;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.execution.AsyncSdkResumeCallback;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;
import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;

import com.google.inject.Inject;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(PIPELINE)
@Slf4j
public class AsyncStrategy implements ExecuteStrategy {
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private AsyncWaitEngine asyncWaitEngine;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    AsyncExecutable asyncExecutable = extractStep(nodeExecution);
    AsyncExecutableResponse asyncExecutableResponse = asyncExecutable.executeAsync(ambiance,
        sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(nodeExecution, asyncExecutableResponse);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    AsyncExecutable asyncExecutable = extractStep(nodeExecution);
    StepResponse stepResponse = asyncExecutable.handleAsyncResponse(ambiance,
        sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution), resumePackage.getResponseDataMap());
    sdkNodeExecutionService.handleStepResponse(
        nodeExecution.getUuid(), StepResponseMapper.toStepResponseProto(stepResponse));
  }

  private void handleResponse(NodeExecutionProto nodeExecution, AsyncExecutableResponse response) {
    PlanNodeProto node = nodeExecution.getNode();
    if (isEmpty(response.getCallbackIdsList())) {
      log.error("StepResponse has no callbackIds - currentState : " + node.getName()
          + ", nodeExecutionId: " + nodeExecution.getUuid());
      throw new InvalidRequestException("Callback Ids cannot be empty for Async Executable Response");
    }

    AsyncSdkResumeCallback callback = AsyncSdkResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    asyncWaitEngine.waitForAllOn(callback, response.getCallbackIdsList().toArray(new String[0]));
    sdkNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), extractStatus(response),
        ExecutableResponse.newBuilder().setAsync(response).build(), Collections.emptyList());
  }

  @Override
  public AsyncExecutable extractStep(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (AsyncExecutable) stepRegistry.obtain(node.getStepType());
  }

  private Status extractStatus(AsyncExecutableResponse response) {
    if (response.getMode() == AsyncExecutableMode.APPROVAL_WAITING_MODE) {
      return Status.APPROVAL_WAITING;
    } else if (response.getMode() == AsyncExecutableMode.RESOURCE_WAITING_MODE) {
      return Status.RESOURCE_WAITING;
    }
    return Status.ASYNC_WAITING;
  }
}
