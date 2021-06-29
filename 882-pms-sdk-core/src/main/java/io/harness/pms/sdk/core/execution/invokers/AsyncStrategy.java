package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableMode;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.execution.AsyncSdkProgressCallback;
import io.harness.pms.sdk.core.execution.AsyncSdkResumeCallback;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ProgressableStrategy;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;
import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(PIPELINE)
@Slf4j
public class AsyncStrategy extends ProgressableStrategy {
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private AsyncWaitEngine asyncWaitEngine;

  @Override
  public void start(InvokerPackage invokerPackage) {
    Ambiance ambiance = invokerPackage.getAmbiance();
    AsyncExecutable asyncExecutable = extractStep(ambiance);
    AsyncExecutableResponse asyncExecutableResponse = asyncExecutable.executeAsync(ambiance,
        invokerPackage.getStepParameters(), invokerPackage.getInputPackage(), invokerPackage.getPassThroughData());
    handleResponse(
        ambiance, invokerPackage.getExecutionMode(), invokerPackage.getStepParameters(), asyncExecutableResponse);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    Ambiance ambiance = resumePackage.getAmbiance();
    AsyncExecutable asyncExecutable = extractStep(ambiance);
    StepResponse stepResponse = asyncExecutable.handleAsyncResponse(
        ambiance, resumePackage.getStepParameters(), resumePackage.getResponseDataMap());
    sdkNodeExecutionService.handleStepResponse(
        AmbianceUtils.obtainCurrentRuntimeId(ambiance), StepResponseMapper.toStepResponseProto(stepResponse));
  }

  private void handleResponse(
      Ambiance ambiance, ExecutionMode mode, StepParameters stepParameters, AsyncExecutableResponse response) {
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    String stepParamString = RecastOrchestrationUtils.toDocumentJson(stepParameters);
    if (isEmpty(response.getCallbackIdsList())) {
      log.error("StepResponse has no callbackIds - currentState : " + AmbianceUtils.obtainStepIdentifier(ambiance)
          + ", nodeExecutionId: " + nodeExecutionId);
      throw new InvalidRequestException("Callback Ids cannot be empty for Async Executable Response");
    }
    AsyncSdkResumeCallback callback = AsyncSdkResumeCallback.builder().nodeExecutionId(nodeExecutionId).build();
    AsyncSdkProgressCallback progressCallback =
        AsyncSdkProgressCallback.builder()
            .ambianceBytes(ambiance.toByteArray())
            .stepParameters(
                stepParamString == null ? new byte[] {} : ByteString.copyFromUtf8(stepParamString).toByteArray())
            .mode(mode)
            .build();
    sdkNodeExecutionService.addExecutableResponse(nodeExecutionId, extractStatus(response),
        ExecutableResponse.newBuilder().setAsync(response).build(), Collections.emptyList());
    asyncWaitEngine.waitForAllOn(callback, progressCallback, response.getCallbackIdsList().toArray(new String[0]));
  }

  @Override
  public AsyncExecutable extractStep(Ambiance ambiance) {
    return (AsyncExecutable) stepRegistry.obtain(AmbianceUtils.getCurrentStepType(ambiance));
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
