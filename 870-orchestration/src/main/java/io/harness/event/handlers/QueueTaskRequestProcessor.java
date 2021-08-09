package io.harness.event.handlers;

import static io.harness.execution.NodeExecution.NodeExecutionKeys;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.resume.EngineResumeCallback;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.engine.progress.EngineProgressCallback;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
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
public class QueueTaskRequestProcessor implements SdkResponseProcessor {
  @Inject private Map<TaskCategory, TaskExecutor> taskExecutorMap;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) private String publisherName;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine orchestrationEngine;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    // Queue Task
    QueueTaskRequest queueTaskRequest = event.getQueueTaskRequest();
    String nodeExecutionId = event.getNodeExecutionId();
    String taskId =
        queueTask(nodeExecutionId, queueTaskRequest.getTaskRequest(), queueTaskRequest.getSetupAbstractionsMap());

    // this indicates and issue in task execution
    if (taskId == null) {
      log.error("Failed to Queue Task. Exiting handler");
      return;
    }
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
      log.info("TaskRequestQueued for NodeExecutionId : {}, TaskId; {}", nodeExecutionId, taskId);
      EngineResumeCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecutionId).build();
      ProgressCallback progressCallback = EngineProgressCallback.builder().nodeExecutionId(nodeExecutionId).build();
      waitNotifyEngine.waitForAllOn(publisherName, callback, progressCallback, taskId);
      return taskId;
    } catch (Exception ex) {
      log.error("Error while queuing delegate task for node execution {}", nodeExecutionId, ex);
      NodeExecution errorNodeExecution = nodeExecutionService.get(nodeExecutionId);
      orchestrationEngine.handleError(errorNodeExecution.getAmbiance(), ex);
      return null;
    }
  }
}
