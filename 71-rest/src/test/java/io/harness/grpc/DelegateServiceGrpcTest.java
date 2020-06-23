package io.harness.grpc;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;
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
import io.harness.category.element.UnitTests;
import io.harness.delegate.AccountId;
import io.harness.delegate.Capability;
import io.harness.delegate.DelegateServiceGrpc;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.perpetualtask.BasicAuthCredentials;
import io.harness.perpetualtask.HttpsPerpetualTaskClientEntrypoint;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskClientEntrypoint;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.DelegateService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@RunWith(MockitoJUnitRunner.class)
public class DelegateServiceGrpcTest extends WingsBaseTest implements MockableTestMixin {
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private DelegateServiceGrpcClient delegateServiceGrpcClient;
  private io.harness.grpc.DelegateServiceGrpc delegateServiceGrpc;

  private PerpetualTaskServiceClientRegistry perpetualTaskServiceClientRegistry;
  private PerpetualTaskService perpetualTaskService;
  private DelegateService delegateService;
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
    delegateServiceGrpcClient = new DelegateServiceGrpcClient(delegateServiceBlockingStub);

    perpetualTaskServiceClientRegistry = mock(PerpetualTaskServiceClientRegistry.class);
    perpetualTaskService = mock(PerpetualTaskService.class);
    delegateService = mock(DelegateService.class);
    delegateServiceGrpc = new io.harness.grpc.DelegateServiceGrpc(
        perpetualTaskServiceClientRegistry, perpetualTaskService, delegateService, kryoSerializer);

    server =
        InProcessServerBuilder.forName(serverName).directExecutor().addService(delegateServiceGrpc).build().start();
    grpcCleanup.register(server);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testSubmitTask() throws InterruptedException {
    ByteString kryoParams = ByteString.copyFrom(kryoSerializer.asDeflatedBytes(ScriptType.BASH));

    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(DelegateTaskKeys.appId, "appId");
    setupAbstractions.put(DelegateTaskKeys.envId, "envId");
    setupAbstractions.put(DelegateTaskKeys.infrastructureMappingId, "infrastructureMappingId");
    setupAbstractions.put(DelegateTaskKeys.serviceTemplateId, "serviceTemplateId");
    setupAbstractions.put(DelegateTaskKeys.artifactStreamId, "artifactStreamId");
    setupAbstractions.put(DelegateTaskKeys.workflowExecutionId, "workflowExecutionId");

    Map<String, String> expressions = new HashMap<>();
    expressions.put("expression1", "exp1");
    expressions.put("expression1", "exp1");

    Capability capability = Capability.newBuilder()
                                .setKryoCapability(ByteString.copyFrom(
                                    kryoSerializer.asDeflatedBytes(SystemEnvCheckerCapability.builder().build())))
                                .build();

    TaskId taskId = delegateServiceGrpcClient.submitTask(AccountId.newBuilder().setId(generateUuid()).build(),
        TaskSetupAbstractions.newBuilder().putAllValues(setupAbstractions).build(),
        TaskDetails.newBuilder()
            .setType(TaskType.newBuilder().setType("TYPE").build())
            .setKryoParameters(kryoParams)
            .setExecutionTimeout(Duration.newBuilder().setSeconds(3).setNanos(100).build())
            .setExpressionFunctorToken(200)
            .putAllExpressions(expressions)
            .build(),
        Arrays.asList(new Capability[] {capability}));
    assertThat(taskId).isNotNull();
    assertThat(taskId.getId()).isNotBlank();
    verify(delegateService).executeTask(any(DelegateTask.class));
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
        .thenReturn(Optional.ofNullable(DelegateTask.builder().status(Status.FINISHED).build()));

    Consumer<TaskExecutionStage> taskExecutionStageConsumer = mock(Consumer.class);

    delegateServiceGrpcClient.taskProgressUpdate(AccountId.newBuilder().setId(accountId).build(),
        TaskId.newBuilder().setId(taskId).build(), taskExecutionStageConsumer);
    verify(taskExecutionStageConsumer).accept(TaskExecutionStage.QUEUEING);
    verify(taskExecutionStageConsumer).accept(TaskExecutionStage.EXECUTING);
    verify(taskExecutionStageConsumer).accept(TaskExecutionStage.FINISHED);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testRegisterPerpetualTaskClientEntrypoint() {
    try {
      delegateServiceGrpcClient.registerPerpetualTaskClientEntrypoint(PerpetualTaskType.SAMPLE,
          PerpetualTaskClientEntrypoint.newBuilder()
              .setHttpsEntrypoint(
                  HttpsPerpetualTaskClientEntrypoint.newBuilder()
                      .setUrl("https://localhost:9999")
                      .setBasicAuthCredentials(
                          BasicAuthCredentials.newBuilder().setUsername("test@harness.io").setPassword("test").build())
                      .build())
              .build());
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
            .putAllTaskClientParams(new HashMap<>())
            .setLastContextUpdated(Timestamps.fromMillis(lastContextUpdated))
            .build();

    PerpetualTaskClientContext context = new PerpetualTaskClientContext(new HashMap<>());
    context.setLastContextUpdated(lastContextUpdated);

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
      delegateServiceGrpcClient.resetPerpetualTask(
          AccountId.newBuilder().build(), PerpetualTaskId.newBuilder().build());
    } catch (Exception e) {
      fail("Should not have thrown any exception");
    }
  }
}
