package io.harness.engine.executables.invokers;

import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.chain.task.TaskChainExecutable;
import io.harness.facilitator.modes.chain.task.TaskChainExecutableResponse;
import io.harness.facilitator.modes.chain.task.TaskChainResponse;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;
import lombok.NonNull;

import java.util.Map;

public class TaskChainExecutableInvoker implements ExecutableInvoker {
  @Inject private Map<String, TaskExecutor> taskExecutorMap;
  @Inject private AmbianceHelper ambianceHelper;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void invokeExecutable(InvokerPackage invokerPackage) {
    TaskChainExecutable taskChainExecutable = (TaskChainExecutable) invokerPackage.getStep();
    Ambiance ambiance = invokerPackage.getAmbiance();
    TaskChainResponse taskChainResponse;
    if (invokerPackage.isStart()) {
      taskChainResponse =
          taskChainExecutable.startChainLink(ambiance, invokerPackage.getParameters(), invokerPackage.getInputs());
    } else {
      taskChainResponse = taskChainExecutable.executeNextLink(ambiance, invokerPackage.getParameters(),
          invokerPackage.getInputs(), invokerPackage.getPassThroughData(), invokerPackage.getResponseDataMap());
    }
    handleResponse(ambiance, taskChainResponse);
  }

  private void handleResponse(@NonNull Ambiance ambiance, @NonNull TaskChainResponse taskChainResponse) {
    Task task = taskChainResponse.getTask();
    NodeExecution nodeExecution = Preconditions.checkNotNull(ambianceHelper.obtainNodeExecution(ambiance));
    TaskExecutor taskExecutor = taskExecutorMap.get(taskChainResponse.getTask().getTaskIdentifier());
    String taskId = Preconditions.checkNotNull(taskExecutor.queueTask(ambiance, taskChainResponse.getTask()));
    // Update Execution Node Instance state to TASK_WAITING
    nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), Status.TASK_WAITING,
        ops
        -> ops.addToSet(NodeExecutionKeys.executableResponses,
            TaskChainExecutableResponse.builder()
                .taskId(taskId)
                .taskIdentifier(task.getTaskIdentifier())
                .taskType(task.getTaskType())
                .chainEnd(taskChainResponse.isChainEnd())
                .passThroughData(taskChainResponse.getPassThroughData())
                .build()));
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, task.getWaitId());
  }
}
