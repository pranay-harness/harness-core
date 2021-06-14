package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.sdk.core.execution.invokers.StrategyHelper.buildResponseDataSupplier;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.execution.ChainDetails;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ProgressableStrategy;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.TaskChainExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Collections;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(PIPELINE)
public class TaskChainStrategy extends ProgressableStrategy {
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private StrategyHelper strategyHelper;

  @Override
  public void start(InvokerPackage invokerPackage) {
    Ambiance ambiance = invokerPackage.getAmbiance();
    TaskChainExecutable taskChainExecutable = extractStep(ambiance);

    TaskChainResponse taskChainResponse = taskChainExecutable.startChainLink(
        ambiance, invokerPackage.getStepParameters(), invokerPackage.getInputPackage());
    handleResponse(ambiance, invokerPackage.getStepParameters(), taskChainResponse);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    Ambiance ambiance = resumePackage.getAmbiance();
    ChainDetails chainDetails = resumePackage.getChainDetails();
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    TaskChainExecutable taskChainExecutable = extractStep(ambiance);
    StepParameters stepParameters = resumePackage.getStepParameters();
    if (chainDetails.isShouldEnd()) {
      StepResponse stepResponse;
      try {
        stepResponse = taskChainExecutable.finalizeExecution(ambiance, stepParameters,
            chainDetails.getPassThroughData(), buildResponseDataSupplier(resumePackage.getResponseDataMap()));
      } catch (Exception e) {
        stepResponse = strategyHelper.handleException(e);
      }
      sdkNodeExecutionService.handleStepResponse(nodeExecutionId, StepResponseMapper.toStepResponseProto(stepResponse));
    } else {
      try {
        TaskChainResponse chainResponse =
            taskChainExecutable.executeNextLink(ambiance, stepParameters, resumePackage.getStepInputPackage(),
                chainDetails.getPassThroughData(), buildResponseDataSupplier(resumePackage.getResponseDataMap()));
        handleResponse(ambiance, stepParameters, chainResponse);
      } catch (Exception e) {
        sdkNodeExecutionService.handleStepResponse(
            nodeExecutionId, StepResponseMapper.toStepResponseProto(strategyHelper.handleException(e)));
      }
    }
  }

  private void handleResponse(
      @NonNull Ambiance ambiance, StepParameters stepParameters, @NonNull TaskChainResponse taskChainResponse) {
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    if (taskChainResponse.isChainEnd() && taskChainResponse.getTaskRequest() == null) {
      TaskChainExecutable taskChainExecutable = extractStep(ambiance);
      sdkNodeExecutionService.addExecutableResponse(nodeExecutionId, Status.NO_OP,
          ExecutableResponse.newBuilder()
              .setTaskChain(TaskChainExecutableResponse.newBuilder()
                                .setChainEnd(true)
                                .setPassThroughData(
                                    ByteString.copyFrom(kryoSerializer.asBytes(taskChainResponse.getPassThroughData())))
                                .addAllLogKeys(CollectionUtils.emptyIfNull(taskChainResponse.getLogKeys()))
                                .addAllUnits(CollectionUtils.emptyIfNull(taskChainResponse.getUnits()))
                                .build())
              .build(),
          Collections.emptyList());
      StepResponse stepResponse = null;
      try {
        stepResponse = taskChainExecutable.finalizeExecution(
            ambiance, stepParameters, taskChainResponse.getPassThroughData(), () -> null);
      } catch (Exception e) {
        stepResponse = strategyHelper.handleException(e);
      }
      sdkNodeExecutionService.handleStepResponse(nodeExecutionId, StepResponseMapper.toStepResponseProto(stepResponse));
      return;
    }
    TaskRequest taskRequest = taskChainResponse.getTaskRequest();

    ExecutableResponse executableResponse =
        ExecutableResponse.newBuilder()
            .setTaskChain(
                TaskChainExecutableResponse.newBuilder()
                    .setTaskCategory(taskChainResponse.getTaskRequest().getTaskCategory())
                    .setChainEnd(taskChainResponse.isChainEnd())
                    .setPassThroughData(
                        ByteString.copyFrom(kryoSerializer.asBytes(taskChainResponse.getPassThroughData())))
                    .addAllLogKeys(CollectionUtils.emptyIfNull(taskRequest.getDelegateTaskRequest().getLogKeysList()))
                    .addAllUnits(CollectionUtils.emptyIfNull(taskRequest.getDelegateTaskRequest().getUnitsList()))
                    .setTaskName(taskRequest.getDelegateTaskRequest().getTaskName())
                    .build())
            .build();
    QueueTaskRequest queueTaskRequest = QueueTaskRequest.newBuilder()
                                            .setNodeExecutionId(nodeExecutionId)
                                            .putAllSetupAbstractions(ambiance.getSetupAbstractionsMap())
                                            .setTaskRequest(taskRequest)
                                            .setExecutableResponse(executableResponse)
                                            .setStatus(Status.TASK_WAITING)
                                            .build();
    sdkNodeExecutionService.queueTaskRequest(queueTaskRequest);
  }

  @Override
  public TaskChainExecutable extractStep(Ambiance ambiance) {
    return (TaskChainExecutable) stepRegistry.obtain(AmbianceUtils.getCurrentStepType(ambiance));
  }
}
