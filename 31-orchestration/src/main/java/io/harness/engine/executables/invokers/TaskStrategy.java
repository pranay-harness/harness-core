package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.OrchestrationPublisherName;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.ResumePackage;
import io.harness.engine.executables.TaskExecuteStrategy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.task.TaskExecutable;
import io.harness.facilitator.modes.task.TaskExecutableResponse;
import io.harness.plan.PlanNode;
import io.harness.registries.state.StepRegistry;
import io.harness.state.io.StepResponse;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;
import io.harness.tasks.TaskMode;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;
import lombok.Builder;
import lombok.NonNull;

import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class TaskStrategy implements TaskExecuteStrategy {
  @Inject private Map<String, TaskExecutor> taskExecutorMap;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private OrchestrationEngine engine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  private TaskMode mode;

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
        ambiance, nodeExecution.getResolvedStepParameters(), invokerPackage.getInputPackage());
    handleResponse(ambiance, task);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecution nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    TaskExecutable taskExecutable = extractTaskExecutable(nodeExecution);
    StepResponse stepResponse = taskExecutable.handleTaskResult(
        ambiance, nodeExecution.getResolvedStepParameters(), resumePackage.getResponseDataMap());
    engine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
  }

  private TaskExecutable extractTaskExecutable(NodeExecution nodeExecution) {
    PlanNode node = nodeExecution.getNode();
    return (TaskExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(@NonNull Ambiance ambiance, Task task) {
    NodeExecution nodeExecution =
        Preconditions.checkNotNull(nodeExecutionService.get(ambiance.obtainCurrentRuntimeId()));
    TaskExecutor taskExecutor = taskExecutorMap.get(mode.name());
    String taskId = Preconditions.checkNotNull(taskExecutor.queueTask(ambiance.getSetupAbstractions(), task));
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, taskId);

    // Update Execution Node Instance state to TASK_WAITING
    nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), Status.TASK_WAITING,
        ops
        -> ops.addToSet(NodeExecutionKeys.executableResponses,
            TaskExecutableResponse.builder().taskId(taskId).taskMode(mode).build()));
  }

  @Override
  public TaskMode getMode() {
    return mode;
  }
}
