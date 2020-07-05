package io.harness.grpc.ng;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;
import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.delegate.TaskId;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

public class DelegateTaskResponseGrpcClientTest extends CategoryTest {
  @Rule public GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();

  private String taskId = "taskId";
  private final SendTaskResultResponse sendTaskResultResponse =
      SendTaskResultResponse.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build();

  private final NgDelegateTaskResponseServiceGrpc
      .NgDelegateTaskResponseServiceImplBase ngDelegateTaskResponseServiceImplBase =
      mock(NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceImplBase.class,
          delegatesTo(new NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceImplBase() {
            public void sendTaskResult(io.harness.delegate.SendTaskResultRequest request,
                io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskResultResponse> responseObserver) {
              responseObserver.onNext(sendTaskResultResponse);
              responseObserver.onCompleted();
            }
          }));

  private NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceBlockingStub ngDelegateTaskServiceBlockingStub;
  private DelegateTaskResponseGrpcClient delegateTaskResponseGrpcClient;

  @Before
  public void doSetup() throws IOException {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanupRule.register(InProcessServerBuilder.forName(serverName)
                                 .directExecutor()
                                 .addService(ngDelegateTaskResponseServiceImplBase)
                                 .build()
                                 .start());

    // Create a client channel and register for automatic graceful shutdown.
    ManagedChannel channel =
        grpcCleanupRule.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());

    ngDelegateTaskServiceBlockingStub = NgDelegateTaskResponseServiceGrpc.newBlockingStub(channel);

    delegateTaskResponseGrpcClient = new DelegateTaskResponseGrpcClient(ngDelegateTaskServiceBlockingStub);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSendTaskResult() {
    SendTaskResultRequest sendTaskResultRequest =
        SendTaskResultRequest.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build();
    SendTaskResultResponse sendTaskResultResponse =
        delegateTaskResponseGrpcClient.sendTaskResult(sendTaskResultRequest);
    assertThat(sendTaskResultResponse).isNotNull();
    assertThat(sendTaskResultResponse).isEqualTo(sendTaskResultResponse);
  }
}
