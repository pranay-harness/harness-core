package io.harness.event.handlers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.tasks.NgDelegate2TaskExecutor;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.engine.progress.EngineProgressCallback;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.events.QueueTaskRequestAndExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.SdkResponseEventInternal;
import io.harness.rule.Owner;
import io.harness.waiter.OldNotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import java.util.List;
import java.util.Map;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class QueueTaskAndAddExecutableResponseHandlerTest extends OrchestrationTestBase {
  @Mock private Map<TaskCategory, TaskExecutor> taskExecutorMap;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private NgDelegate2TaskExecutor ngDelegate2TaskExecutor;
  @Mock private NodeExecutionService nodeExecutionService;

  @InjectMocks private QueueTaskAndAddExecutableResponseHandler queueTaskAndAddExecutableResponseHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyMocks() {
    verifyNoMoreInteractions(nodeExecutionService);
    verifyNoMoreInteractions(waitNotifyEngine);
    verifyNoMoreInteractions(taskExecutorMap);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEvent() {
    when(taskExecutorMap.get(TaskCategory.DELEGATE_TASK_V2)).thenReturn(ngDelegate2TaskExecutor);
    when(ngDelegate2TaskExecutor.queueTask(any(), any(), any())).thenReturn("taskId");
    QueueTaskRequest queueTaskRequest =
        QueueTaskRequest.newBuilder()
            .setNodeExecutionId("id")
            .setTaskRequest(TaskRequest.newBuilder().setTaskCategory(TaskCategory.DELEGATE_TASK_V2).build())
            .build();
    AddExecutableResponseRequest request =
        AddExecutableResponseRequest.newBuilder().setNodeExecutionId("id").setStatus(Status.NO_OP).build();

    queueTaskAndAddExecutableResponseHandler.handleEvent(
        SdkResponseEventInternal.builder()
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setQueueTaskRequestAndExecutableResponseRequest(
                                             QueueTaskRequestAndExecutableResponseRequest.newBuilder()
                                                 .setQueueTaskRequest(queueTaskRequest)
                                                 .setAddExecutableResponseRequest(request)
                                                 .build())
                                         .buildPartial())
            .build());

    verify(nodeExecutionService).update(eq("id"), any());
    verify(taskExecutorMap).get(queueTaskRequest.getTaskRequest().getTaskCategory());
    verify(waitNotifyEngine)
        .waitForAllOn(null,
            EngineResumeCallback.builder().nodeExecutionId(queueTaskRequest.getNodeExecutionId()).build(),
            EngineProgressCallback.builder().nodeExecutionId(queueTaskRequest.getNodeExecutionId()).build(), "taskId");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEventWithStatus() {
    when(taskExecutorMap.get(TaskCategory.DELEGATE_TASK_V2)).thenReturn(ngDelegate2TaskExecutor);
    when(ngDelegate2TaskExecutor.queueTask(any(), any(), any())).thenReturn("taskId");
    QueueTaskRequest queueTaskRequest =
        QueueTaskRequest.newBuilder()
            .setNodeExecutionId("id")
            .setTaskRequest(TaskRequest.newBuilder().setTaskCategory(TaskCategory.DELEGATE_TASK_V2).build())
            .build();
    AddExecutableResponseRequest request =
        AddExecutableResponseRequest.newBuilder().setNodeExecutionId("id").setStatus(Status.SUCCEEDED).build();

    queueTaskAndAddExecutableResponseHandler.handleEvent(
        SdkResponseEventInternal.builder()
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setQueueTaskRequestAndExecutableResponseRequest(
                                             QueueTaskRequestAndExecutableResponseRequest.newBuilder()
                                                 .setQueueTaskRequest(queueTaskRequest)
                                                 .setAddExecutableResponseRequest(request)
                                                 .build())
                                         .buildPartial())
            .build());
    verify(nodeExecutionService).updateStatusWithOps(eq("id"), eq(Status.SUCCEEDED), any());

    verify(taskExecutorMap).get(queueTaskRequest.getTaskRequest().getTaskCategory());
    verify(waitNotifyEngine)
        .waitForAllOn(null,
            EngineResumeCallback.builder().nodeExecutionId(queueTaskRequest.getNodeExecutionId()).build(),
            EngineProgressCallback.builder().nodeExecutionId(queueTaskRequest.getNodeExecutionId()).build(), "taskId");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEventWithCallbackIds() {
    when(taskExecutorMap.get(TaskCategory.DELEGATE_TASK_V2)).thenReturn(ngDelegate2TaskExecutor);
    when(ngDelegate2TaskExecutor.queueTask(any(), any(), any())).thenReturn("taskId");
    List<String> callbackIds = Lists.newArrayList("callbackId1");
    AddExecutableResponseRequest request = AddExecutableResponseRequest.newBuilder()
                                               .setNodeExecutionId("id")
                                               .setStatus(Status.SUCCEEDED)
                                               .addAllCallbackIds(callbackIds)
                                               .build();

    QueueTaskRequest queueTaskRequest =
        QueueTaskRequest.newBuilder()
            .setNodeExecutionId("id")
            .setTaskRequest(TaskRequest.newBuilder().setTaskCategory(TaskCategory.DELEGATE_TASK_V2).build())
            .build();

    queueTaskAndAddExecutableResponseHandler.handleEvent(
        SdkResponseEventInternal.builder()
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setQueueTaskRequestAndExecutableResponseRequest(
                                             QueueTaskRequestAndExecutableResponseRequest.newBuilder()
                                                 .setQueueTaskRequest(queueTaskRequest)
                                                 .setAddExecutableResponseRequest(request)
                                                 .build())
                                         .buildPartial())
            .build());

    OldNotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(request.getNodeExecutionId()).build();

    verify(waitNotifyEngine).waitForAllOn(null, callback, callbackIds.toArray(new String[0]));

    verify(nodeExecutionService).updateStatusWithOps(eq("id"), eq(Status.SUCCEEDED), any());

    verify(taskExecutorMap).get(queueTaskRequest.getTaskRequest().getTaskCategory());
    verify(waitNotifyEngine)
        .waitForAllOn(null,
            EngineResumeCallback.builder().nodeExecutionId(queueTaskRequest.getNodeExecutionId()).build(),
            EngineProgressCallback.builder().nodeExecutionId(queueTaskRequest.getNodeExecutionId()).build(), "taskId");
  }
}