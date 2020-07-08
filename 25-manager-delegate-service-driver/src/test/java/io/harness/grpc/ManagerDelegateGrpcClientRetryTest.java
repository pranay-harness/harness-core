package io.harness.grpc;

import static io.grpc.Status.UNAUTHENTICATED;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AccountId;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.SendTaskAsyncRequest;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.TaskId;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.stream.IntStream;

@Slf4j
public class ManagerDelegateGrpcClientRetryTest {
  @Rule public GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();

  @Rule public ExpectedException thrown = ExpectedException.none();

  private final String ACCOUNT_ID = generateUuid();
  private final SendTaskRequest sendTaskRequest =
      SendTaskRequest.newBuilder().setAccountId(AccountId.newBuilder().setId(ACCOUNT_ID).build()).build();

  private final SendTaskAsyncResponse sendTaskAsyncResponse =
      SendTaskAsyncResponse.newBuilder().setTaskId(TaskId.newBuilder().setId("test").build()).build();

  private final SendTaskResponse sendTaskResponse =
      SendTaskResponse.newBuilder().setTaskId(TaskId.newBuilder().setId("test").build()).build();

  private final SendTaskAsyncRequest sendTaskAsyncRequest =
      SendTaskAsyncRequest.newBuilder().setAccountId(AccountId.newBuilder().setId(ACCOUNT_ID).build()).build();

  private final NgDelegateTaskServiceGrpc.NgDelegateTaskServiceImplBase ngDelegateTaskServiceImplBase =
      mock(NgDelegateTaskServiceGrpc.NgDelegateTaskServiceImplBase.class,
          delegatesTo(new NgDelegateTaskServiceGrpc.NgDelegateTaskServiceImplBase() {
            public void sendTask(SendTaskRequest request, StreamObserver<SendTaskResponse> responseObserver) {
              responseObserver.onError(new StatusRuntimeException(UNAUTHENTICATED));
            }
            public void sendTaskAsync(SendTaskAsyncRequest request,
                StreamObserver<io.harness.delegate.SendTaskAsyncResponse> responseObserver) {
              responseObserver.onNext(sendTaskAsyncResponse);
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
  public void testRetry() {
    SendTaskRequest sendTaskRequest =
        SendTaskRequest.newBuilder().setAccountId(AccountId.newBuilder().setId(ACCOUNT_ID).build()).build();

    int failureCount = 2;
    IntStream.range(0, failureCount).forEach(i -> {
      try {
        managerDelegateGrpcClient.sendTask(sendTaskRequest);
      } catch (Exception ex) {
        logger.debug("Exception occurred when making grpc call");
      }
    });

    assertThat(managerDelegateGrpcClient.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    assertThat(managerDelegateGrpcClient.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(failureCount);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testCircuitBreaker() {
    assertThat(managerDelegateGrpcClient.getCircuitBreakerState()).isEqualTo(CircuitBreaker.State.CLOSED);
    int failureCount = 43;
    IntStream.range(0, failureCount).forEach(i -> {
      try {
        managerDelegateGrpcClient.sendTask(sendTaskRequest);
      } catch (Exception ex) {
        logger.debug("Exception occurred when making grpc call");
      }
    });

    assertThat(managerDelegateGrpcClient.getCircuitBreakerState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThat(managerDelegateGrpcClient.getNumberOfFailedCalls()).isEqualTo(100);
  }
}
