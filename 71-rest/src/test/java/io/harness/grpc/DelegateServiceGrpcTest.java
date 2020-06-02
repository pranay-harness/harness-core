package io.harness.grpc;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.Duration;
import com.google.protobuf.util.Timestamps;

import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AccountId;
import io.harness.delegate.DelegateServiceGrpc;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskClientEntrypoint;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class DelegateServiceGrpcTest extends CategoryTest implements MockableTestMixin {
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private DelegateServiceGrpcClient delegateServiceGrpcClient;
  private io.harness.grpc.DelegateServiceGrpc delegateServiceGrpc;

  private PerpetualTaskService perpetualTaskService;
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

    perpetualTaskService = mock(PerpetualTaskService.class);
    delegateServiceGrpc = new io.harness.grpc.DelegateServiceGrpc(perpetualTaskService);

    server =
        InProcessServerBuilder.forName(serverName).directExecutor().addService(delegateServiceGrpc).build().start();
    grpcCleanup.register(server);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testSubmitTask() {
    TaskId taskId = delegateServiceGrpcClient.submitTask(
        TaskSetupAbstractions.newBuilder().build(), TaskDetails.newBuilder().build(), Collections.emptyList());
    assertThat(taskId).isNotNull();
    assertThat(taskId.getId()).isNullOrEmpty();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testCancelTask() {
    TaskExecutionStage taskExecutionStage = delegateServiceGrpcClient.cancelTask(TaskId.newBuilder().build());
    assertThat(taskExecutionStage).isNotNull();
    assertThat(taskExecutionStage).isEqualTo(TaskExecutionStage.TYPE_UNSPECIFIED);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testTaskProgress() {
    TaskExecutionStage taskExecutionStage = delegateServiceGrpcClient.taskProgress(TaskId.newBuilder().build());
    assertThat(taskExecutionStage).isNotNull();
    assertThat(taskExecutionStage).isEqualTo(TaskExecutionStage.TYPE_UNSPECIFIED);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testTaskProgressUpdates() {
    List<TaskExecutionStage> taskExecutionStages =
        delegateServiceGrpcClient.taskProgressUpdate(TaskId.newBuilder().build());
    assertThat(taskExecutionStages).isNotNull();
    assertThat(taskExecutionStages.size()).isEqualTo(1);
    assertThat(taskExecutionStages.get(0)).isEqualTo(TaskExecutionStage.TYPE_UNSPECIFIED);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testRegisterPerpetualTaskClientEntrypoint() {
    try {
      delegateServiceGrpcClient.registerPerpetualTaskClientEntrypoint(
          PerpetualTaskType.SAMPLE.name(), PerpetualTaskClientEntrypoint.newBuilder().build());
    } catch (Exception e) {
      fail("Should not have thrown any exception");
    }
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testCreatePerpetualTask() {
    String accountId = generateUuid();
    PerpetualTaskType type = PerpetualTaskType.SAMPLE;
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
        AccountId.newBuilder().setId(accountId).build(), type.name(), schedule, contextDetails, false);

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
