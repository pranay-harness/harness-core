package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.execution.Status.TASK_WAITING;

import io.harness.AmbianceUtils;
import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
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
import io.harness.pms.execution.TaskExecutableResponse;
import io.harness.pms.execution.TaskMode;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.registries.state.StepRegistry;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.ProgressCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class TaskStrategy implements TaskExecuteStrategy {
  @Inject private Map<String, TaskExecutor> taskExecutorMap;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private OrchestrationEngine engine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  private final TaskMode mode;

  @Builder
  public TaskStrategy(TaskMode mode) {
    this.mode = mode;
  }

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecution nodeExecution = invokerPackage.getNodeExecution();
    TaskExecutable taskExecutable = extractTaskExecutable(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance();
    Task task = taskExecutable.obtainTask(
        ambiance, nodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(ambiance, task);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecution nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    TaskExecutable taskExecutable = extractTaskExecutable(nodeExecution);
    StepResponse stepResponse = taskExecutable.handleTaskResult(ambiance,
        nodeExecutionService.extractResolvedStepParameters(nodeExecution), resumePackage.getResponseDataMap());
    engine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
  }

  private TaskExecutable extractTaskExecutable(NodeExecution nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (TaskExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(@NonNull Ambiance ambiance, Task task) {
    NodeExecution nodeExecution =
        Preconditions.checkNotNull(nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance)));
    TaskExecutor taskExecutor = taskExecutorMap.get(mode.name());
    String taskId = Preconditions.checkNotNull(taskExecutor.queueTask(ambiance.getSetupAbstractionsMap(), task));
    NotifyCallback notifyCallback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    ProgressCallback progressCallback =
        EngineProgressCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, notifyCallback, progressCallback, taskId);

    // Update Execution Node Instance state to TASK_WAITING
    nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), TASK_WAITING,
        ops
        -> ops.addToSet(NodeExecutionKeys.executableResponses,
            ExecutableResponse.newBuilder().setTask(
                TaskExecutableResponse.newBuilder().setTaskId(taskId).setTaskMode(mode).build())));
  }

  @Override
  public TaskMode getMode() {
    return mode;
  }
}
