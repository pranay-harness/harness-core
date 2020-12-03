package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.EngineObtainmentHelper;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.ResumePackage;
import io.harness.engine.executables.TaskExecuteStrategy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.progress.EngineProgressCallback;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.TaskChainExecutableResponse;
import io.harness.pms.execution.TaskMode;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.data.Metadata;
import io.harness.pms.sdk.core.steps.executables.TaskChainExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.registries.state.StepRegistry;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.TaskExecutor;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.ProgressCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class TaskChainStrategy implements TaskExecuteStrategy {
  @Inject private Map<String, TaskExecutor> taskExecutorMap;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private OrchestrationEngine engine;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject private KryoSerializer kryoSerializer;

  private final TaskMode mode;

  public TaskChainStrategy(TaskMode mode) {
    this.mode = mode;
  }

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecution nodeExecution = invokerPackage.getNodeExecution();
    TaskChainExecutable taskChainExecutable = extractTaskChainExecutable(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance();
    TaskChainResponse taskChainResponse;
    taskChainResponse = taskChainExecutable.startChainLink(
        ambiance, nodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(ambiance, nodeExecution, taskChainResponse);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecution nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    TaskChainExecutable taskChainExecutable = extractTaskChainExecutable(nodeExecution);
    TaskChainExecutableResponse lastLinkResponse =
        Objects.requireNonNull(nodeExecution.obtainLatestExecutableResponse()).getTaskChain();
    if (lastLinkResponse.getChainEnd()) {
      StepResponse stepResponse = taskChainExecutable.finalizeExecution(ambiance,
          nodeExecutionService.extractResolvedStepParameters(nodeExecution),
          (PassThroughData) kryoSerializer.asObject(lastLinkResponse.getPassThroughData().toByteArray()),
          resumePackage.getResponseDataMap());
      engine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
    } else {
      StepInputPackage inputPackage =
          engineObtainmentHelper.obtainInputPackage(ambiance, nodeExecution.getNode().getRebObjectsList());
      TaskChainResponse chainResponse = taskChainExecutable.executeNextLink(ambiance,
          nodeExecutionService.extractResolvedStepParameters(nodeExecution), inputPackage,
          (PassThroughData) kryoSerializer.asObject(lastLinkResponse.getPassThroughData().toByteArray()),
          resumePackage.getResponseDataMap());
      handleResponse(ambiance, nodeExecution, chainResponse);
    }
  }

  private TaskChainExecutable extractTaskChainExecutable(NodeExecution nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (TaskChainExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(
      @NonNull Ambiance ambiance, NodeExecution nodeExecution, @NonNull TaskChainResponse taskChainResponse) {
    if (taskChainResponse.isChainEnd() && taskChainResponse.getTask() == null) {
      TaskChainExecutable taskChainExecutable = extractTaskChainExecutable(nodeExecution);
      nodeExecutionService.update(nodeExecution.getUuid(),
          ops
          -> ops.addToSet(NodeExecutionKeys.executableResponses,
              ExecutableResponse.newBuilder()
                  .setTaskChain(TaskChainExecutableResponse.newBuilder()
                                    .setTaskId(null)
                                    .setTaskMode(null)
                                    .setChainEnd(true)
                                    .setPassThroughData(ByteString.copyFrom(
                                        kryoSerializer.asBytes(taskChainResponse.getPassThroughData())))
                                    .build())
                  .setMetadata(taskChainResponse.getMetadata() == null ? new Metadata() {}.toJson()
                                                                       : taskChainResponse.getMetadata().toJson())
                  .build()));
      StepResponse stepResponse = taskChainExecutable.finalizeExecution(ambiance,
          nodeExecutionService.extractResolvedStepParameters(nodeExecution), taskChainResponse.getPassThroughData(),
          null);
      engine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
      return;
    }
    TaskExecutor taskExecutor = taskExecutorMap.get(mode.name());
    String taskId = Preconditions.checkNotNull(
        taskExecutor.queueTask(ambiance.getSetupAbstractionsMap(), taskChainResponse.getTask()));
    // Update Execution Node Instance state to TASK_WAITING
    nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), Status.TASK_WAITING,
        ops
        -> ops.addToSet(NodeExecutionKeys.executableResponses,
            ExecutableResponse.newBuilder()
                .setTaskChain(TaskChainExecutableResponse.newBuilder()
                                  .setTaskId(taskId)
                                  .setTaskMode(mode)
                                  .setChainEnd(taskChainResponse.isChainEnd())
                                  .setPassThroughData(ByteString.copyFrom(
                                      kryoSerializer.asBytes(taskChainResponse.getPassThroughData())))
                                  .build())
                .setMetadata(taskChainResponse.getMetadata() == null ? new Metadata() {}.toJson()
                                                                     : taskChainResponse.getMetadata().toJson())
                .build()));
    NotifyCallback notifyCallback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    ProgressCallback progressCallback =
        EngineProgressCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, notifyCallback, progressCallback, taskId);
  }

  @Override
  public TaskMode getMode() {
    return mode;
  }
}
