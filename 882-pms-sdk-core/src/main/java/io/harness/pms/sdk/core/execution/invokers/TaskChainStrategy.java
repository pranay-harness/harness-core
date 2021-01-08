package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.NodeExecutionUtils;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.TaskChainExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;
import io.harness.serializer.KryoSerializer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.Objects;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class TaskChainStrategy implements ExecuteStrategy {
  @Inject private PmsNodeExecutionService pmsNodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    TaskChainExecutable taskChainExecutable = extractTaskChainExecutable(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance();
    TaskChainResponse taskChainResponse;
    taskChainResponse = taskChainExecutable.startChainLink(ambiance,
        pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(ambiance, nodeExecution, taskChainResponse);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    TaskChainExecutable taskChainExecutable = extractTaskChainExecutable(nodeExecution);
    TaskChainExecutableResponse lastLinkResponse =
        Objects.requireNonNull(NodeExecutionUtils.obtainLatestExecutableResponse(nodeExecution)).getTaskChain();
    if (lastLinkResponse.getChainEnd()) {
      StepResponse stepResponse = taskChainExecutable.finalizeExecution(ambiance,
          pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution),
          (PassThroughData) kryoSerializer.asObject(lastLinkResponse.getPassThroughData().toByteArray()),
          resumePackage.getResponseDataMap());
      pmsNodeExecutionService.handleStepResponse(
          nodeExecution.getUuid(), StepResponseMapper.toStepResponseProto(stepResponse));
    } else {
      StepInputPackage inputPackage =
          engineObtainmentHelper.obtainInputPackage(ambiance, nodeExecution.getNode().getRebObjectsList());
      TaskChainResponse chainResponse = taskChainExecutable.executeNextLink(ambiance,
          pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), inputPackage,
          (PassThroughData) kryoSerializer.asObject(lastLinkResponse.getPassThroughData().toByteArray()),
          resumePackage.getResponseDataMap());
      handleResponse(ambiance, nodeExecution, chainResponse);
    }
  }

  private TaskChainExecutable extractTaskChainExecutable(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (TaskChainExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(
      @NonNull Ambiance ambiance, NodeExecutionProto nodeExecution, @NonNull TaskChainResponse taskChainResponse) {
    if (taskChainResponse.isChainEnd() && taskChainResponse.getTaskRequest() == null) {
      TaskChainExecutable taskChainExecutable = extractTaskChainExecutable(nodeExecution);
      pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.UNRECOGNIZED,
          ExecutableResponse.newBuilder()
              .setTaskChain(TaskChainExecutableResponse.newBuilder()
                                .setChainEnd(true)
                                .setPassThroughData(
                                    ByteString.copyFrom(kryoSerializer.asBytes(taskChainResponse.getPassThroughData())))
                                .build())
              .build(),
          Collections.emptyList());
      StepResponse stepResponse = taskChainExecutable.finalizeExecution(ambiance,
          pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), taskChainResponse.getPassThroughData(),
          null);
      pmsNodeExecutionService.handleStepResponse(
          nodeExecution.getUuid(), StepResponseMapper.toStepResponseProto(stepResponse));
      return;
    }

    String taskId = Preconditions.checkNotNull(pmsNodeExecutionService.queueTask(
        nodeExecution.getUuid(), ambiance.getSetupAbstractionsMap(), taskChainResponse.getTaskRequest()));
    // Update Execution Node Instance state to TASK_WAITING
    TaskRequest taskRequest = taskChainResponse.getTaskRequest();
    pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.TASK_WAITING,
        ExecutableResponse.newBuilder()
            .setTaskChain(
                TaskChainExecutableResponse.newBuilder()
                    .setTaskId(taskId)
                    .setTaskCategory(taskChainResponse.getTaskRequest().getTaskCategory())
                    .setChainEnd(taskChainResponse.isChainEnd())
                    .setPassThroughData(
                        ByteString.copyFrom(kryoSerializer.asBytes(taskChainResponse.getPassThroughData())))
                    .addAllLogKeys(CollectionUtils.emptyIfNull(taskRequest.getDelegateTaskRequest().getLogKeysList()))
                    .addAllUnits(CollectionUtils.emptyIfNull(taskRequest.getDelegateTaskRequest().getUnitsList()))
                    .build())
            .build(),
        Collections.emptyList());
  }
}
