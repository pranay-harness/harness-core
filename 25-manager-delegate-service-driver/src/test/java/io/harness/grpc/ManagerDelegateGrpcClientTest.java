package io.harness.grpc;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AbortTaskRequest;
import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.AccountId;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.SendTaskAsyncRequest;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

public class ManagerDelegateGrpcClientTest extends CategoryTest {
  @Rule public GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();

  private static final String ACCOUNT_ID = generateUuid();

  private final SendTaskResponse sendTaskResponse =
      SendTaskResponse.newBuilder().setTaskId(TaskId.newBuilder().setId("test").build()).build();

  private final SendTaskAsyncResponse sendTaskAsyncResponse =
      SendTaskAsyncResponse.newBuilder().setTaskId(TaskId.newBuilder().setId("test").build()).build();
  private final AbortTaskResponse abortTaskResponse =
      AbortTaskResponse.newBuilder().setCanceledAtStage(TaskExecutionStage.EXECUTING).build();

  private final NgDelegateTaskServiceGrpc.NgDelegateTaskServiceImplBase ngDelegateTaskServiceImplBase =
      mock(NgDelegateTaskServiceGrpc.NgDelegateTaskServiceImplBase.class,
          delegatesTo(new NgDelegateTaskServiceGrpc.NgDelegateTaskServiceImplBase() {
            public void sendTask(SendTaskRequest request, StreamObserver<SendTaskResponse> responseObserver) {
              responseObserver.onNext(sendTaskResponse);
              responseObserver.onCompleted();
            }

            public void sendTaskAsync(SendTaskAsyncRequest request,
                StreamObserver<io.harness.delegate.SendTaskAsyncResponse> responseObserver) {
              responseObserver.onNext(sendTaskAsyncResponse);
              responseObserver.onCompleted();
            }

            public void abortTask(io.harness.delegate.AbortTaskRequest request,
                io.grpc.stub.StreamObserver<io.harness.delegate.AbortTaskResponse> responseObserver) {
              responseObserver.onNext(abortTaskResponse);
              responseObserver.onCompleted();
            }
          }));

  private NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub;

  private ManagerDelegateGrpcClient managerDelegateGrpcClient;

  @Before
  public void doSetup() throws IOException {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanupRule.register(InProcessServerBuilder.forName(serverName)
                                 .directExecutor()
                                 .addService(ngDelegateTaskServiceImplBase)
                                 .build()
                                 .start());

    // Create a client channel and register for automatic graceful shutdown.
    ManagedChannel channel =
        grpcCleanupRule.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());

    ngDelegateTaskServiceBlockingStub = NgDelegateTaskServiceGrpc.newBlockingStub(channel);

    managerDelegateGrpcClient = new ManagerDelegateGrpcClient(ngDelegateTaskServiceBlockingStub);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSendTask() {
    SendTaskRequest sendTaskRequest =
        SendTaskRequest.newBuilder().setAccountId(AccountId.newBuilder().setId(ACCOUNT_ID).build()).build();
    SendTaskResponse taskResponse = managerDelegateGrpcClient.sendTask(sendTaskRequest);
    assertThat(taskResponse).isNotNull();
    assertThat(taskResponse).isEqualTo(sendTaskResponse);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSendTaskAsync() {
    SendTaskAsyncRequest sendTaskAsyncRequest =
        SendTaskAsyncRequest.newBuilder().setAccountId(AccountId.newBuilder().setId(ACCOUNT_ID).build()).build();
    SendTaskAsyncResponse taskResponse = managerDelegateGrpcClient.sendTaskAsync(sendTaskAsyncRequest);
    assertThat(taskResponse).isNotNull();
    assertThat(taskResponse).isEqualTo(sendTaskAsyncResponse);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testAbortTask() {
    AbortTaskRequest abortTaskRequest = AbortTaskRequest.newBuilder().build();
    AbortTaskResponse taskResponse = managerDelegateGrpcClient.abortTask(abortTaskRequest);
    assertThat(taskResponse).isNotNull();
    assertThat(taskResponse).isEqualTo(abortTaskResponse);
  }
}
