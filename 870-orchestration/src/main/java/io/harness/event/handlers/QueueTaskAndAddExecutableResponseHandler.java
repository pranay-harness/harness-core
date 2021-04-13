package io.harness.event.handlers;

import static io.harness.execution.NodeExecution.NodeExecutionKeys;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.decorators.ExecutableResponseDecorator;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.engine.progress.EngineProgressCallback;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.execution.SdkResponseEventInternal;
import io.harness.waiter.OldNotifyCallback;
import io.harness.waiter.ProgressCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class QueueTaskAndAddExecutableResponseHandler implements SdkResponseEventHandler {
  @Inject private Map<TaskCategory, TaskExecutor> taskExecutorMap;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) private String publisherName;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void handleEvent(SdkResponseEventInternal event) {
    // Queue Task
    QueueTaskRequest queueTaskRequest =
        event.getSdkResponseEventRequest().getQueueTaskRequestAndExecutableResponseRequest().getQueueTaskRequest();
    String taskId = queueTask(queueTaskRequest);

    // Add Executable Response
    AddExecutableResponseRequest addExecutableResponseRequest = event.getSdkResponseEventRequest()
                                                                    .getQueueTaskRequestAndExecutableResponseRequest()
                                                                    .getAddExecutableResponseRequest();
    ExecutableResponseDecorator executableResponseDecorator =
        ExecutableResponseDecorator.builder().taskId(taskId).build();
    ExecutableResponse executableResponse =
        executableResponseDecorator.decorate(addExecutableResponseRequest.getExecutableResponse());
    addExecutableResponse(addExecutableResponseRequest.getNodeExecutionId(), addExecutableResponseRequest.getStatus(),
        executableResponse, new ArrayList<>(addExecutableResponseRequest.getCallbackIdsList()));
  }

  private String queueTask(QueueTaskRequest queueTaskRequest) {
    try {
      TaskExecutor taskExecutor = taskExecutorMap.get(queueTaskRequest.getTaskRequest().getTaskCategory());
      String taskId = Preconditions.checkNotNull(taskExecutor.queueTask(
          queueTaskRequest.getSetupAbstractionsMap(), queueTaskRequest.getTaskRequest(), Duration.ofSeconds(0)));
      OldNotifyCallback callback =
          EngineResumeCallback.builder().nodeExecutionId(queueTaskRequest.getNodeExecutionId()).build();
      ProgressCallback progressCallback =
          EngineProgressCallback.builder().nodeExecutionId(queueTaskRequest.getNodeExecutionId()).build();
      waitNotifyEngine.waitForAllOn(publisherName, callback, progressCallback, taskId);
      return taskId;
    } catch (Exception ex) {
      log.error("Error while queuing delegate task", ex);
      throw ex;
    }
  }

  private void addExecutableResponse(
      String nodeExecutionId, Status status, ExecutableResponse executableResponse, List<String> callbackIds) {
    if (EmptyPredicate.isNotEmpty(callbackIds)) {
      OldNotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecutionId).build();
      waitNotifyEngine.waitForAllOn(publisherName, callback, callbackIds.toArray(new String[0]));
    }

    if (status == Status.NO_OP) {
      nodeExecutionService.update(
          nodeExecutionId, ops -> ops.addToSet(NodeExecutionKeys.executableResponses, executableResponse));
    } else {
      nodeExecutionService.updateStatusWithOps(
          nodeExecutionId, status, ops -> ops.addToSet(NodeExecutionKeys.executableResponses, executableResponse));
    }
  }
}
