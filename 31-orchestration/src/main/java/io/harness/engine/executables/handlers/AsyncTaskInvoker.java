package io.harness.engine.executables.handlers;

import static io.harness.execution.status.NodeExecutionStatus.TASK_WAITING;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.EngineStatusHelper;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.facilitator.modes.task.AsyncTaskExecutable;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;
import lombok.NonNull;

import java.util.Map;

public class AsyncTaskInvoker implements ExecutableInvoker {
  @Inject private Map<String, TaskExecutor> taskExecutorMap;
  @Inject private AmbianceHelper ambianceHelper;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private EngineStatusHelper engineStatusHelper;

  @Override
  public void invokeExecutable(InvokerPackage invokerPackage) {
    AsyncTaskExecutable asyncTaskExecutable = (AsyncTaskExecutable) invokerPackage.getState();
    Ambiance ambiance = invokerPackage.getAmbiance();
    Task task = asyncTaskExecutable.obtainTask(ambiance, invokerPackage.getParameters(), invokerPackage.getInputs());
    handleResponse(ambiance, task);
  }

  private void handleResponse(@NonNull Ambiance ambiance, @NonNull Task task) {
    NodeExecution nodeExecution = Preconditions.checkNotNull(ambianceHelper.obtainNodeExecution(ambiance));
    TaskExecutor taskExecutor = taskExecutorMap.get(task.getClass().getCanonicalName());
    String taskId = Preconditions.checkNotNull(taskExecutor.queueTask(ambiance, task));
    NotifyCallback callback = EngineResumeCallback.builder().nodeInstanceId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, task.getWaitId());

    // Update Execution Node Instance state to TASK_WAITING
    engineStatusHelper.updateNodeInstance(nodeExecution.getUuid(),
        ops -> ops.set(NodeExecutionKeys.status, TASK_WAITING).set(NodeExecutionKeys.taskId, taskId));
  }
}
