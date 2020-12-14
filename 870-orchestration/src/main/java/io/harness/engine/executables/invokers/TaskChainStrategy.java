package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.EngineObtainmentHelper;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.ResumePackage;
import io.harness.engine.executables.TaskExecuteStrategy;
import io.harness.execution.NodeExecutionUtils;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.execution.NodeExecutionProto;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.TaskChainExecutableResponse;
import io.harness.pms.execution.TaskMode;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.data.Metadata;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.steps.executables.TaskChainExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.serializer.KryoSerializer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.Objects;
import lombok.NonNull;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class TaskChainStrategy implements TaskExecuteStrategy {
  @Inject private PmsNodeExecutionService pmsNodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private KryoSerializer kryoSerializer;

  private final TaskMode mode;

  public TaskChainStrategy(TaskMode mode) {
    this.mode = mode;
  }

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
      pmsNodeExecutionService.handleStepResponse(nodeExecution.getUuid(), stepResponse);
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
    if (taskChainResponse.isChainEnd() && taskChainResponse.getTask() == null) {
      TaskChainExecutable taskChainExecutable = extractTaskChainExecutable(nodeExecution);
      pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.UNRECOGNIZED,
          ExecutableResponse.newBuilder()
              .setTaskChain(TaskChainExecutableResponse.newBuilder()
                                .setChainEnd(true)
                                .setPassThroughData(
                                    ByteString.copyFrom(kryoSerializer.asBytes(taskChainResponse.getPassThroughData())))
                                .build())
              .setMetadata(taskChainResponse.getMetadata() == null ? new Metadata() {}.toJson()
                                                                   : taskChainResponse.getMetadata().toJson())
              .build(),
          Collections.emptyList());
      StepResponse stepResponse = taskChainExecutable.finalizeExecution(ambiance,
          pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), taskChainResponse.getPassThroughData(),
          null);
      pmsNodeExecutionService.handleStepResponse(nodeExecution.getUuid(), stepResponse);
      return;
    }

    String taskId = Preconditions.checkNotNull(pmsNodeExecutionService.queueTask(
        nodeExecution.getUuid(), mode, ambiance.getSetupAbstractionsMap(), taskChainResponse.getTask()));
    // Update Execution Node Instance state to TASK_WAITING
    pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.TASK_WAITING,
        ExecutableResponse.newBuilder()
            .setTaskChain(TaskChainExecutableResponse.newBuilder()
                              .setTaskId(taskId)
                              .setTaskMode(mode)
                              .setChainEnd(taskChainResponse.isChainEnd())
                              .setPassThroughData(
                                  ByteString.copyFrom(kryoSerializer.asBytes(taskChainResponse.getPassThroughData())))
                              .build())
            .setMetadata(taskChainResponse.getMetadata() == null ? new Metadata() {}.toJson()
                                                                 : taskChainResponse.getMetadata().toJson())
            .build(),
        Collections.emptyList());
  }

  @Override
  public TaskMode getMode() {
    return mode;
  }
}
