package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.OrchestrationPublisherName;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.TaskInvokeStrategy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.task.TaskExecutable;
import io.harness.facilitator.modes.task.TaskExecutableResponse;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;
import io.harness.tasks.TaskMode;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;
import lombok.Builder;
import lombok.NonNull;

import java.util.Map;

@OwnedBy(CDC)
public class TaskStrategy implements TaskInvokeStrategy {
  @Inject private Map<String, TaskExecutor> taskExecutorMap;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  private TaskMode mode;

  @Builder
  public TaskStrategy(TaskMode mode) {
    this.mode = mode;
  }

  @Override
  public void invoke(InvokerPackage invokerPackage) {
    TaskExecutable taskExecutable = (TaskExecutable) invokerPackage.getStep();
    Ambiance ambiance = invokerPackage.getAmbiance();
    Task task = taskExecutable.obtainTask(ambiance, invokerPackage.getParameters(), invokerPackage.getInputPackage());
    handleResponse(ambiance, task);
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
