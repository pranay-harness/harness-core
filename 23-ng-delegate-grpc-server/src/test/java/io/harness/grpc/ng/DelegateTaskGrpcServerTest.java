package io.harness.grpc.ng;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AbortTaskRequest;
import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub;
import io.harness.delegate.SendTaskAsyncRequest;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

public class DelegateTaskGrpcServerTest extends CategoryTest {
  @Rule public GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();

  private NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub;

  @Before
  public void doSetup() throws IOException {
    String serverName = InProcessServerBuilder.generateName();

    grpcCleanupRule.register(InProcessServerBuilder.forName(serverName)
                                 .directExecutor()
                                 .addService(new DelegateTaskGrpcServer())
                                 .build()
                                 .start());

    ngDelegateTaskServiceBlockingStub = NgDelegateTaskServiceGrpc.newBlockingStub(
        grpcCleanupRule.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
  }

  @Test
  @Owner(developers = OwnerRule.VIKAS)
  @Category(UnitTests.class)
  public void testAllMethods() {
    String taskId = "task_task_Id";

    SendTaskResponse sendTaskResponse = ngDelegateTaskServiceBlockingStub.sendTask(
        SendTaskRequest.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build());
    assertThat(sendTaskResponse).isNotNull();
    assertThat(sendTaskResponse.getTaskId()).isNotNull();
    assertThat(sendTaskResponse.getTaskId().getId()).isEqualTo(taskId);

    SendTaskAsyncResponse sendTaskAsyncResponse = ngDelegateTaskServiceBlockingStub.sendTaskAsync(
        SendTaskAsyncRequest.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build());
    assertThat(sendTaskAsyncResponse).isNotNull();
    assertThat(sendTaskAsyncResponse.getTaskId()).isNotNull();
    assertThat(sendTaskAsyncResponse.getTaskId().getId()).isEqualTo(taskId);

    AbortTaskResponse abortTaskResponse = ngDelegateTaskServiceBlockingStub.abortTask(
        AbortTaskRequest.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build());
    assertThat(abortTaskResponse).isNotNull();
    assertThat(abortTaskResponse.getCanceledAtStage()).isEqualTo(TaskExecutionStage.EXECUTING);
  }
}
