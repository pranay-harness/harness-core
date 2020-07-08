package io.harness.ng.core.remote.server.grpc;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceBlockingStub;
import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.delegate.TaskId;
import io.harness.ng.core.BaseTest;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.WaitNotifyEngine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.io.IOException;

public class NgDelegateTaskResponseGrpcServerTest extends BaseTest {
  @Rule public GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();

  private NgDelegateTaskResponseServiceBlockingStub ngDelegateTaskServiceBlockingStub;

  @Inject private KryoSerializer kryoSerializer;
  @Mock private WaitNotifyEngine waitNotifyEngine;

  @Before
  public void doSetup() throws IOException {
    String serverName = InProcessServerBuilder.generateName();

    grpcCleanupRule.register(InProcessServerBuilder.forName(serverName)
                                 .directExecutor()
                                 .addService(new NgDelegateTaskResponseGrpcServer(waitNotifyEngine, kryoSerializer))
                                 .build()
                                 .start());

    ngDelegateTaskServiceBlockingStub = NgDelegateTaskResponseServiceGrpc.newBlockingStub(
        grpcCleanupRule.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSendTaskResult() {
    String taskId = "task_task_Id";

    SendTaskResultResponse sendTaskResultResponse = ngDelegateTaskServiceBlockingStub.sendTaskResult(
        SendTaskResultRequest.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build());

    assertThat(sendTaskResultResponse).isNotNull();
    assertThat(sendTaskResultResponse.getAcknowledgement()).isTrue();
  }
}
