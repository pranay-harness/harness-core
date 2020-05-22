package io.harness.grpc;

import static io.harness.rule.OwnerRule.MARKO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.category.element.UnitTests;
import io.harness.delegate.DelegateServiceGrpc;
import io.harness.delegate.TaskCapabilities;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class DelegateServiceGrpcTest extends CategoryTest implements MockableTestMixin {
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private DelegateServiceGrpcClient delegateServiceGrpcClient;
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
    server = InProcessServerBuilder.forName(serverName)
                 .directExecutor()
                 .addService(new io.harness.grpc.DelegateServiceGrpc())
                 .build()
                 .start();
    grpcCleanup.register(server);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testSubmitTask() {
    TaskId taskId = delegateServiceGrpcClient.submitTask(TaskSetupAbstractions.newBuilder().build(),
        TaskDetails.newBuilder().build(), TaskCapabilities.newBuilder().build());
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
}
