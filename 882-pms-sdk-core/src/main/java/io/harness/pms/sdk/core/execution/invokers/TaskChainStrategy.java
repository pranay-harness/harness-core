package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.sdk.core.execution.invokers.StrategyHelper.buildResponseDataSupplier;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.NodeExecutionUtils;
import io.harness.pms.sdk.core.execution.ProgressableStrategy;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.TaskChainExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.Objects;
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
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    TaskChainExecutable taskChainExecutable = extractStep(nodeExecution);

    Ambiance ambiance = nodeExecution.getAmbiance();
    TaskChainResponse taskChainResponse;
    taskChainResponse = taskChainExecutable.startChainLink(ambiance,
        sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(ambiance, nodeExecution, taskChainResponse);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    TaskChainExecutable taskChainExecutable = extractStep(nodeExecution);
    TaskChainExecutableResponse lastLinkResponse =
        Objects.requireNonNull(NodeExecutionUtils.obtainLatestExecutableResponse(nodeExecution)).getTaskChain();
    if (lastLinkResponse.getChainEnd()) {
      StepResponse stepResponse = null;
      try {
        stepResponse = taskChainExecutable.finalizeExecution(ambiance,
            sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution),
            (PassThroughData) kryoSerializer.asObject(lastLinkResponse.getPassThroughData().toByteArray()),
            buildResponseDataSupplier(resumePackage.getResponseDataMap()));
      } catch (Exception e) {
        stepResponse = strategyHelper.handleException(e);
      }
      sdkNodeExecutionService.handleStepResponse(
          nodeExecution.getUuid(), StepResponseMapper.toStepResponseProto(stepResponse));
    } else {
      StepInputPackage inputPackage =
          engineObtainmentHelper.obtainInputPackage(ambiance, nodeExecution.getNode().getRebObjectsList());
      TaskChainResponse chainResponse = null;
      try {
        chainResponse = taskChainExecutable.executeNextLink(ambiance,
            sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution), inputPackage,
            (PassThroughData) kryoSerializer.asObject(lastLinkResponse.getPassThroughData().toByteArray()),
            buildResponseDataSupplier(resumePackage.getResponseDataMap()));
        handleResponse(ambiance, nodeExecution, chainResponse);
      } catch (Exception e) {
        sdkNodeExecutionService.handleStepResponse(
            nodeExecution.getUuid(), StepResponseMapper.toStepResponseProto(strategyHelper.handleException(e)));
      }
    }
  }

  private void handleResponse(
      @NonNull Ambiance ambiance, NodeExecutionProto nodeExecution, @NonNull TaskChainResponse taskChainResponse) {
    if (taskChainResponse.isChainEnd() && taskChainResponse.getTaskRequest() == null) {
      TaskChainExecutable taskChainExecutable = extractStep(nodeExecution);
      sdkNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.NO_OP,
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
        stepResponse = taskChainExecutable.finalizeExecution(ambiance,
            sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution),
            taskChainResponse.getPassThroughData(), () -> null);
      } catch (Exception e) {
        stepResponse = strategyHelper.handleException(e);
      }
      sdkNodeExecutionService.handleStepResponse(
          nodeExecution.getUuid(), StepResponseMapper.toStepResponseProto(stepResponse));
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
                                            .setNodeExecutionId(nodeExecution.getUuid())
                                            .putAllSetupAbstractions(ambiance.getSetupAbstractionsMap())
                                            .setTaskRequest(taskRequest)
                                            .setExecutableResponse(executableResponse)
                                            .setStatus(Status.TASK_WAITING)
                                            .build();
    sdkNodeExecutionService.queueTaskRequest(queueTaskRequest);
  }

  @Override
  public TaskChainExecutable extractStep(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (TaskChainExecutable) stepRegistry.obtain(node.getStepType());
  }
}
