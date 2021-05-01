package io.harness.event.handlers;

import static io.harness.execution.NodeExecution.NodeExecutionKeys;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.engine.progress.EngineProgressCallback;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.SdkResponseEventInternal;
import io.harness.waiter.OldNotifyCallback;
import io.harness.waiter.ProgressCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class QueueTaskResponseHandler implements SdkResponseEventHandler {
  @Inject private Map<TaskCategory, TaskExecutor> taskExecutorMap;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) private String publisherName;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void handleEvent(SdkResponseEventInternal event) {
    // Queue Task
    QueueTaskRequest queueTaskRequest = event.getSdkResponseEventRequest().getQueueTaskRequest();
    String nodeExecutionId = queueTaskRequest.getNodeExecutionId();
    String taskId =
        queueTask(nodeExecutionId, queueTaskRequest.getTaskRequest(), queueTaskRequest.getSetupAbstractionsMap());
    ExecutableResponse executableResponse = buildExecutableResponseWithTaskId(queueTaskRequest, taskId);
    // Add Executable Response
    nodeExecutionService.updateStatusWithOps(nodeExecutionId, queueTaskRequest.getStatus(),
        ops -> ops.addToSet(NodeExecutionKeys.executableResponses, executableResponse), EnumSet.noneOf(Status.class));
  }

  private ExecutableResponse buildExecutableResponseWithTaskId(QueueTaskRequest queueTaskRequest, String taskId) {
    ExecutableResponse executableResponse;
    ExecutableResponse requestExecutableResponse = queueTaskRequest.getExecutableResponse();
    switch (requestExecutableResponse.getResponseCase()) {
      case TASKCHAIN:
        TaskChainExecutableResponse.Builder taskChainBuilder = requestExecutableResponse.getTaskChain().toBuilder();
        executableResponse =
            requestExecutableResponse.toBuilder().setTaskChain(taskChainBuilder.setTaskId(taskId).build()).build();
        break;
      case TASK:
        TaskExecutableResponse.Builder taskBuilder = requestExecutableResponse.getTask().toBuilder();
        executableResponse =
            requestExecutableResponse.toBuilder().setTask(taskBuilder.setTaskId(taskId).build()).build();
        break;
      default:
        throw new InvalidRequestException(
            "Executable Response Case is not handled" + requestExecutableResponse.getResponseCase());
    }
    return executableResponse;
  }

  private String queueTask(String nodeExecutionId, TaskRequest taskRequest, Map<String, String> setupAbstractionsMap) {
    try {
      TaskExecutor taskExecutor = taskExecutorMap.get(taskRequest.getTaskCategory());
      String taskId =
          Preconditions.checkNotNull(taskExecutor.queueTask(setupAbstractionsMap, taskRequest, Duration.ofSeconds(0)));
      OldNotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecutionId).build();
      ProgressCallback progressCallback = EngineProgressCallback.builder().nodeExecutionId(nodeExecutionId).build();
      waitNotifyEngine.waitForAllOn(publisherName, callback, progressCallback, taskId);
      return taskId;
    } catch (Exception ex) {
      log.error("Error while queuing delegate task", ex);
      throw ex;
    }
  }
}
