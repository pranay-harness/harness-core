package io.harness.grpc;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.SANJA;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import com.google.protobuf.util.Timestamps;

import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.MockableTestMixin;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.DelegateTask.Status;
import io.harness.callback.DelegateCallback;
import io.harness.callback.DelegateCallbackToken;
import io.harness.callback.MongoDatabase;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AccountId;
import io.harness.delegate.DelegateServiceGrpc;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.TaskClientParams;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateCallbackRegistry;
import io.harness.tasks.Cd1SetupFields;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.DelegateService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@RunWith(MockitoJUnitRunner.class)
public class DelegateServiceGrpcImplTest extends WingsBaseTest implements MockableTestMixin {
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private DelegateServiceGrpcClient delegateServiceGrpcClient;
  private DelegateServiceGrpcImpl delegateServiceGrpcImpl;

  private PerpetualTaskService perpetualTaskService;
  private DelegateService delegateService;
  @Inject private DelegateCallbackRegistry delegateCallbackRegistry;
  @Inject private KryoSerializer kryoSerializer;

  private Server server;
  private Logger mockClientLogger;
  private Logger mockServerLogger;

  @Before
  public void setUp() throws Exception {
    mockClientLogger = mock(Logger.class);
    mockServerLogger = mock(Logger.class);
    setStaticFieldValue(DelegateServiceGrpcClient.class, "logger", mockClientLogger);
    setStaticFieldValue(DelegateServiceGrpcClient.class, "logger", mockServerLogger);

    String serverName = InProcessServerBuilder.generateName();
    Channel channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).build());

    DelegateServiceGrpc.DelegateServiceBlockingStub delegateServiceBlockingStub =
        DelegateServiceGrpc.newBlockingStub(channel);
    delegateServiceGrpcClient = new DelegateServiceGrpcClient(delegateServiceBlockingStub, kryoSerializer);

    perpetualTaskService = mock(PerpetualTaskService.class);
    delegateService = mock(DelegateService.class);
    delegateServiceGrpcImpl =
        new DelegateServiceGrpcImpl(delegateCallbackRegistry, perpetualTaskService, delegateService, kryoSerializer);

    server =
        InProcessServerBuilder.forName(serverName).directExecutor().addService(delegateServiceGrpcImpl).build().start();
    grpcCleanup.register(server);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testSubmitTask() throws InterruptedException {
    ByteString kryoParams = ByteString.copyFrom(kryoSerializer.asDeflatedBytes(ScriptType.BASH));

    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(Cd1SetupFields.APP_ID_FIELD, "appId");
    setupAbstractions.put(Cd1SetupFields.ENV_ID_FIELD, "envId");
    setupAbstractions.put(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, "infrastructureMappingId");
    setupAbstractions.put(Cd1SetupFields.SERVICE_TEMPLATE_ID_FIELD, "serviceTemplateId");
    setupAbstractions.put(Cd1SetupFields.ARTIFACT_STREAM_ID_FIELD, "artifactStreamId");
    setupAbstractions.put(DelegateTaskKeys.workflowExecutionId, "workflowExecutionId");

    Map<String, String> expressions = new HashMap<>();
    expressions.put("expression1", "exp1");
    expressions.put("expression1", "exp1");

    TaskDetails.Builder builder = TaskDetails.newBuilder()
                                      .setType(TaskType.newBuilder().setType("TYPE").build())
                                      .setKryoParameters(kryoParams)
                                      .setExecutionTimeout(Duration.newBuilder().setSeconds(3).setNanos(100).build())
                                      .setExpressionFunctorToken(200)
                                      .putAllExpressions(expressions);

    TaskId taskId1 = delegateServiceGrpcClient.submitTask(DelegateCallbackToken.newBuilder().setToken("token").build(),
        AccountId.newBuilder().setId(generateUuid()).build(),
        TaskSetupAbstractions.newBuilder().putAllValues(setupAbstractions).build(),
        builder.setMode(TaskMode.SYNC).build(), asList(SystemEnvCheckerCapability.builder().build()));
    assertThat(taskId1).isNotNull();
    assertThat(taskId1.getId()).isNotBlank();
    verify(delegateService).scheduleSyncTask(any(DelegateTask.class));

    TaskId taskId2 = delegateServiceGrpcClient.submitTask(DelegateCallbackToken.newBuilder().setToken("token").build(),
        AccountId.newBuilder().setId(generateUuid()).build(),
        TaskSetupAbstractions.newBuilder().putAllValues(setupAbstractions).build(),
        builder.setMode(TaskMode.ASYNC).build(), asList(SystemEnvCheckerCapability.builder().build()));
    assertThat(taskId2).isNotNull();
    assertThat(taskId2.getId()).isNotBlank();
    verify(delegateService).queueTask(any(DelegateTask.class));
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testCancelTask() {
    String accountId = generateUuid();
    String taskId = generateUuid();
    when(delegateService.abortTask(accountId, taskId))
        .thenReturn(null)
        .thenReturn(DelegateTask.builder().status(Status.STARTED).build());

    TaskExecutionStage taskExecutionStage = delegateServiceGrpcClient.cancelTask(
        AccountId.newBuilder().setId(accountId).build(), TaskId.newBuilder().setId(taskId).build());
    assertThat(taskExecutionStage).isNotNull();
    assertThat(taskExecutionStage).isEqualTo(TaskExecutionStage.TYPE_UNSPECIFIED);

    taskExecutionStage = delegateServiceGrpcClient.cancelTask(
        AccountId.newBuilder().setId(accountId).build(), TaskId.newBuilder().setId(taskId).build());
    assertThat(taskExecutionStage).isNotNull();
    assertThat(taskExecutionStage).isEqualTo(TaskExecutionStage.EXECUTING);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testTaskProgress() {
    String accountId = generateUuid();
    String taskId = generateUuid();
    when(delegateService.fetchDelegateTask(accountId, taskId))
        .thenReturn(Optional.ofNullable(null))
        .thenReturn(Optional.ofNullable(DelegateTask.builder().status(Status.ERROR).build()));

    TaskExecutionStage taskExecutionStage = delegateServiceGrpcClient.taskProgress(
        AccountId.newBuilder().setId(accountId).build(), TaskId.newBuilder().setId(taskId).build());
    assertThat(taskExecutionStage).isNotNull();
    assertThat(taskExecutionStage).isEqualTo(TaskExecutionStage.TYPE_UNSPECIFIED);

    taskExecutionStage = delegateServiceGrpcClient.taskProgress(
        AccountId.newBuilder().setId(accountId).build(), TaskId.newBuilder().setId(taskId).build());
    assertThat(taskExecutionStage).isNotNull();
    assertThat(taskExecutionStage).isEqualTo(TaskExecutionStage.FAILED);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testTaskProgressUpdatesWhenNoTaskFound() {
    String accountId = generateUuid();
    String taskId = generateUuid();
    when(delegateService.fetchDelegateTask(accountId, taskId)).thenReturn(Optional.ofNullable(null));

    Consumer<TaskExecutionStage> taskExecutionStageConsumer = mock(Consumer.class);

    delegateServiceGrpcClient.taskProgressUpdate(AccountId.newBuilder().setId(accountId).build(),
        TaskId.newBuilder().setId(taskId).build(), taskExecutionStageConsumer);
    verify(taskExecutionStageConsumer).accept(TaskExecutionStage.TYPE_UNSPECIFIED);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testTaskProgressUpdatesWithMultipleStatusChanges() {
    String accountId = generateUuid();
    String taskId = generateUuid();
    when(delegateService.fetchDelegateTask(accountId, taskId))
        .thenReturn(Optional.ofNullable(DelegateTask.builder().status(Status.QUEUED).build()))
        .thenReturn(Optional.ofNullable(DelegateTask.builder().status(Status.STARTED).build()))
        .thenReturn(Optional.ofNullable(null));

    Consumer<TaskExecutionStage> taskExecutionStageConsumer = mock(Consumer.class);

    delegateServiceGrpcClient.taskProgressUpdate(AccountId.newBuilder().setId(accountId).build(),
        TaskId.newBuilder().setId(taskId).build(), taskExecutionStageConsumer);
    verify(taskExecutionStageConsumer).accept(TaskExecutionStage.QUEUEING);
    verify(taskExecutionStageConsumer).accept(TaskExecutionStage.EXECUTING);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testRegisterCallback() {
    try {
      DelegateCallbackToken token = delegateServiceGrpcClient.registerCallback(
          DelegateCallback.newBuilder()
              .setMongoDatabase(
                  MongoDatabase.newBuilder().setConnection("test").setCollectionNamePrefix("test").build())
              .build());
      assertThat(token).isNotNull();
    } catch (Exception e) {
      fail("Should not have thrown any exception");
    }
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testCreatePerpetualTask() {
    String accountId = generateUuid();
    String type = PerpetualTaskType.SAMPLE;
    long lastContextUpdated = 1000L;
    String taskId = generateUuid();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Duration.newBuilder().setSeconds(5).build())
                                         .setTimeout(Duration.newBuilder().setSeconds(2).build())
                                         .build();

    PerpetualTaskClientContextDetails contextDetails =
        PerpetualTaskClientContextDetails.newBuilder()
            .setTaskClientParams(TaskClientParams.newBuilder().putAllParams(new HashMap<>()).build())
            .setLastContextUpdated(Timestamps.fromMillis(lastContextUpdated))
            .build();

    PerpetualTaskClientContext context = PerpetualTaskClientContext.builder()
                                             .clientParams(new HashMap<>())
                                             .lastContextUpdated(lastContextUpdated)
                                             .build();

    when(perpetualTaskService.createTask(eq(type), eq(accountId), eq(context), eq(schedule), eq(false)))
        .thenReturn(taskId);

    PerpetualTaskId perpetualTaskId = delegateServiceGrpcClient.createPerpetualTask(
        AccountId.newBuilder().setId(accountId).build(), type, schedule, contextDetails, false);

    assertThat(perpetualTaskId).isNotNull();
    assertThat(perpetualTaskId.getId()).isEqualTo(taskId);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDeletePerpetualTask() {
    try {
      delegateServiceGrpcClient.deletePerpetualTask(
          AccountId.newBuilder().build(), PerpetualTaskId.newBuilder().build());
    } catch (Exception e) {
      fail("Should not have thrown any exception");
    }
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testResetPerpetualTask() {
    try {
      delegateServiceGrpcClient.resetPerpetualTask(AccountId.newBuilder().build(), PerpetualTaskId.getDefaultInstance(),
          PerpetualTaskExecutionBundle.getDefaultInstance());
    } catch (Exception e) {
      fail("Should not have thrown any exception");
    }
  }
}
