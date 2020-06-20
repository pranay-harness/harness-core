package io.harness.engine.executables.invokers;

import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.task.TaskExecutable;
import io.harness.facilitator.modes.task.TaskExecutableResponse;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;
import lombok.NonNull;

import java.util.Map;

public class TaskExecutableInvoker implements ExecutableInvoker {
  @Inject private Map<String, TaskExecutor> taskExecutorMap;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void invokeExecutable(InvokerPackage invokerPackage) {
    TaskExecutable taskExecutable = (TaskExecutable) invokerPackage.getStep();
    Ambiance ambiance = invokerPackage.getAmbiance();
    Task task = taskExecutable.obtainTask(ambiance, invokerPackage.getParameters(), invokerPackage.getInputs());
    handleResponse(ambiance, task);
  }

  private void handleResponse(@NonNull Ambiance ambiance, @NonNull Task task) {
    NodeExecution nodeExecution =
        Preconditions.checkNotNull(nodeExecutionService.get(ambiance.obtainCurrentRuntimeId()));
    TaskExecutor taskExecutor = taskExecutorMap.get(task.getTaskIdentifier());
    String taskId = Preconditions.checkNotNull(taskExecutor.queueTask(ambiance, task));
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, task.getWaitId());

    // Update Execution Node Instance state to TASK_WAITING
    nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), Status.TASK_WAITING,
        ops
        -> ops.addToSet(NodeExecutionKeys.executableResponses,
            TaskExecutableResponse.builder().taskId(taskId).taskIdentifier(task.getTaskIdentifier()).build()));
  }
}
