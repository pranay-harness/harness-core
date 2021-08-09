package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.contracts.execution.Status.SUSPENDED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.ChildChainExecutableResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.events.SpawnChildRequest;
import io.harness.pms.contracts.execution.events.SuspendChainRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.execution.ChainDetails;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.ChildChainExecutable;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ResponseData;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(PIPELINE)
public class ChildChainStrategy implements ExecuteStrategy {
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private StrategyHelper strategyHelper;
  @Inject private ResponseDataMapper responseDataMapper;

  @Override
  public void start(InvokerPackage invokerPackage) {
    Ambiance ambiance = invokerPackage.getAmbiance();
    ChildChainExecutable childChainExecutable = extractStep(ambiance);
    ChildChainExecutableResponse childChainResponse;
    childChainResponse = childChainExecutable.executeFirstChild(
        ambiance, invokerPackage.getStepParameters(), invokerPackage.getInputPackage());
    handleResponse(ambiance, childChainResponse);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    Ambiance ambiance = resumePackage.getAmbiance();
    ChildChainExecutable childChainExecutable = extractStep(ambiance);
    ChainDetails chainDetails = resumePackage.getChainDetails();
    Map<String, ResponseData> accumulatedResponse = resumePackage.getResponseDataMap();
    if (chainDetails.isShouldEnd()) {
      StepResponse stepResponse = childChainExecutable.finalizeExecution(
          ambiance, resumePackage.getStepParameters(), chainDetails.getPassThroughData(), accumulatedResponse);
      sdkNodeExecutionService.handleStepResponse(ambiance.getPlanExecutionId(),
          AmbianceUtils.obtainCurrentRuntimeId(ambiance), StepResponseMapper.toStepResponseProto(stepResponse));
    } else {
      ChildChainExecutableResponse chainResponse =
          childChainExecutable.executeNextChild(ambiance, resumePackage.getStepParameters(),
              resumePackage.getStepInputPackage(), chainDetails.getPassThroughData(), accumulatedResponse);
      handleResponse(ambiance, chainResponse);
    }
  }

  @Override
  public ChildChainExecutable extractStep(Ambiance ambiance) {
    return (ChildChainExecutable) stepRegistry.obtain(AmbianceUtils.getCurrentStepType(ambiance));
  }

  private void handleResponse(Ambiance ambiance, ChildChainExecutableResponse childChainResponse) {
    if (childChainResponse.getSuspend()) {
      suspendChain(ambiance, childChainResponse);
    } else {
      executeChild(ambiance, childChainResponse);
    }
  }

  private void executeChild(Ambiance ambiance, ChildChainExecutableResponse childChainResponse) {
    SpawnChildRequest spawnChildRequest = SpawnChildRequest.newBuilder().setChildChain(childChainResponse).build();
    sdkNodeExecutionService.spawnChild(
        ambiance.getPlanExecutionId(), AmbianceUtils.obtainCurrentRuntimeId(ambiance), spawnChildRequest);
  }

  private void suspendChain(Ambiance ambiance, ChildChainExecutableResponse childChainResponse) {
    Level currentLevel = Preconditions.checkNotNull(AmbianceUtils.obtainCurrentLevel(ambiance));
    Map<String, ByteString> responseBytes =
        responseDataMapper.toResponseDataProto(Collections.singletonMap("ignore-" + currentLevel.getRuntimeId(),
            StepResponseNotifyData.builder()
                .nodeUuid(currentLevel.getSetupId())
                .identifier(currentLevel.getIdentifier())
                .group(currentLevel.getGroup())
                .status(SUSPENDED)
                .description("Ignoring Execution as next child found to be null")
                .build()));
    sdkNodeExecutionService.suspendChainExecution(ambiance.getPlanExecutionId(), currentLevel.getRuntimeId(),
        SuspendChainRequest.newBuilder()
            .setExecutableResponse(ExecutableResponse.newBuilder().setChildChain(childChainResponse).build())
            .setIsError(false)
            .putAllResponse(responseBytes)
            .build());
  }
}
