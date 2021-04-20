package io.harness.engine.interrupts.helpers;

import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.tasks.TaskCategory;

import com.google.inject.Inject;
import java.util.Map;

public class InterruptHelper {
  @Inject private Map<TaskCategory, TaskExecutor> taskExecutorMap;

  public boolean discontinueTaskIfRequired(NodeExecution nodeExecution) {
    Ambiance ambiance = nodeExecution.getAmbiance();
    ExecutableResponse executableResponse = nodeExecution.obtainLatestExecutableResponse();
    if (executableResponse != null && nodeExecution.isTaskSpawningMode()) {
      String taskId;
      TaskCategory taskCategory;
      switch (executableResponse.getResponseCase()) {
        case TASK:
          TaskExecutableResponse taskExecutableResponse = executableResponse.getTask();
          taskId = taskExecutableResponse.getTaskId();
          taskCategory = taskExecutableResponse.getTaskCategory();
          break;
        case TASKCHAIN:
          TaskChainExecutableResponse taskChainExecutableResponse = executableResponse.getTaskChain();
          taskId = taskChainExecutableResponse.getTaskId();
          taskCategory = taskChainExecutableResponse.getTaskCategory();
          break;
        default:
          throw new InvalidRequestException("Executable Response should contain either task or taskChain");
      }
      TaskExecutor executor = taskExecutorMap.get(taskCategory);
      return executor.abortTask(ambiance.getSetupAbstractionsMap(), taskId);
    }
    return true;
  }
}
