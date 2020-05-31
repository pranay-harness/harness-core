package io.harness.engine.executables.handlers;

import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.engine.services.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
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
  @Inject private AmbianceHelper ambianceHelper;
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
    NodeExecution nodeExecution = Preconditions.checkNotNull(ambianceHelper.obtainNodeExecution(ambiance));
    TaskExecutor taskExecutor = taskExecutorMap.get(task.getTaskIdentifier());
    String taskId = Preconditions.checkNotNull(taskExecutor.queueTask(ambiance, task));
    NotifyCallback callback = EngineResumeCallback.builder().nodeInstanceId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, task.getWaitId());

    // Update Execution Node Instance state to TASK_WAITING
    nodeExecutionService.update(nodeExecution.getUuid(),
        ops
        -> ops.set(NodeExecutionKeys.executableResponse,
            TaskExecutableResponse.builder().taskId(taskId).taskIdentifier(task.getTaskIdentifier()).build()));
  }
}
