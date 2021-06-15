package io.harness.event.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.Status.TASK_WAITING;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
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
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.TaskExecutableResponse.Builder;
import io.harness.pms.contracts.execution.events.QueueTaskRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;

import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.commons.collections4.CollectionUtils;
import org.joor.Reflect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PIPELINE)
public class QueueTaskResponseHandlerTest extends OrchestrationTestBase {
  private Map<TaskCategory, TaskExecutor> taskExecutorMap = new HashMap<>();
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private NgDelegate2TaskExecutor ngDelegate2TaskExecutor;
  @Mock private NodeExecutionService nodeExecutionService;

  @InjectMocks private QueueTaskResponseHandler queueTaskResponseHandler;

  @Captor ArgumentCaptor<String> nExIDCaptor;
  @Captor ArgumentCaptor<Status> sCaptor;
  @Captor ArgumentCaptor<Consumer<Update>> uCaptor;
  @Captor ArgumentCaptor<EnumSet<Status>> esCaptor;
  String taskId = generateUuid();

  @Before
  public void setup() {
    when(ngDelegate2TaskExecutor.queueTask(any(), any(TaskRequest.class), any(Duration.class))).thenReturn(taskId);
    taskExecutorMap.put(TaskCategory.DELEGATE_TASK_V2, ngDelegate2TaskExecutor);
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyMocks() {
    verifyNoMoreInteractions(nodeExecutionService);
    verifyNoMoreInteractions(waitNotifyEngine);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEvent() {
    Map<TaskCategory, TaskExecutor> spy = spy(taskExecutorMap);
    Reflect.on(queueTaskResponseHandler).set("taskExecutorMap", spy);
    String nodeExecutionId = generateUuid();

    Builder taskBuilder = TaskExecutableResponse.newBuilder()
                              .setTaskCategory(TaskCategory.DELEGATE_TASK_V2)
                              .addAllLogKeys(CollectionUtils.emptyCollection())
                              .addAllUnits(CollectionUtils.emptyCollection())
                              .setTaskName("DUMMY_TASK_NAME");

    QueueTaskRequest queueTaskRequest =
        QueueTaskRequest.newBuilder()
            .setNodeExecutionId(nodeExecutionId)
            .setTaskRequest(TaskRequest.newBuilder().setTaskCategory(TaskCategory.DELEGATE_TASK_V2).build())
            .setStatus(TASK_WAITING)
            .setExecutableResponse(ExecutableResponse.newBuilder().setTask(taskBuilder.build()).build())
            .build();

    queueTaskResponseHandler.handleEvent(SdkResponseEventProto.newBuilder()
                                             .setSdkResponseEventType(SdkResponseEventType.QUEUE_TASK)
                                             .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                                                             .setQueueTaskRequest(queueTaskRequest)
                                                                             .setNodeExecutionId(nodeExecutionId)
                                                                             .buildPartial())
                                             .build());

    verify(nodeExecutionService)
        .updateStatusWithOps(nExIDCaptor.capture(), sCaptor.capture(), uCaptor.capture(), esCaptor.capture());

    Update update = new Update();

    assertThat(nExIDCaptor.getValue()).isEqualTo(nodeExecutionId);
    assertThat(sCaptor.getValue()).isEqualTo(TASK_WAITING);
    uCaptor.getValue().accept(update);
    assertThat(update.getUpdateObject().keySet()).containsExactly("$addToSet");
    assertThat(update.getUpdateObject().values()).hasSize(1);
    verify(waitNotifyEngine)
        .waitForAllOn(null, EngineResumeCallback.builder().nodeExecutionId(nodeExecutionId).build(),
            EngineProgressCallback.builder().nodeExecutionId(nodeExecutionId).build(), taskId);
  }
}