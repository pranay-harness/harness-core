package io.harness;

import static io.grpc.Status.UNAVAILABLE;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

import com.google.inject.Inject;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.stream.IntStream;

@Slf4j
public class NgManagerServiceDriverRetryTest extends NgManagerServiceDriverBaseTest {
  @Rule public GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();

  private String taskId = "taskId";
  private final SendTaskResultResponse sendTaskResultResponse =
      SendTaskResultResponse.newBuilder().setAcknowledgement(true).build();

  private final NgDelegateTaskResponseServiceGrpc
      .NgDelegateTaskResponseServiceImplBase ngDelegateTaskResponseServiceImplBase =
      mock(NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceImplBase.class,
          delegatesTo(new NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceImplBase() {
            public void sendTaskResult(io.harness.delegate.SendTaskResultRequest request,
                io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskResultResponse> responseObserver) {
              responseObserver.onError(new StatusRuntimeException(UNAVAILABLE));
            }
          }));

  private NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceBlockingStub ngDelegateTaskServiceBlockingStub;
  private NgManagerServiceDriver ngManagerServiceDriver;
  @Inject private KryoSerializer kryoSerializer;

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

    ngManagerServiceDriver = new NgManagerServiceDriver(ngDelegateTaskServiceBlockingStub, kryoSerializer);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSendTaskResult() {
    int failureCount = 1;
    IntStream.range(0, failureCount).forEach(i -> {
      try {
        ngManagerServiceDriver.sendTaskResult(taskId, null);
      } catch (Exception ex) {
        logger.debug("Exception occurred when making sendTaskResult grpc call");
      }
    });

    assertThat(ngManagerServiceDriver.getNumberOfFailedCallsWithoutRetryAttempt()).isEqualTo(0);
    assertThat(ngManagerServiceDriver.getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(failureCount);
  }
}
