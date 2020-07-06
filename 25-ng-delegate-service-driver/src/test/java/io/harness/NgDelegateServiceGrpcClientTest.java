package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

import com.google.inject.Inject;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AbortTaskRequest;
import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub;
import io.harness.delegate.NgDelegateTaskServiceGrpc.NgDelegateTaskServiceImplBase;
import io.harness.delegate.SendTaskAsyncRequest;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NgDelegateServiceGrpcClientTest extends NgDelegateServiceDriverTest {
  @Rule public GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();

  private static final String ACCOUNT_ID = generateUuid();
  private static final String APP_ID = generateUuid();

  private final SendTaskResponse sendTaskResponse =
      SendTaskResponse.newBuilder().setTaskId(TaskId.newBuilder().setId("test").build()).build();

  private final SendTaskAsyncResponse sendTaskAsyncResponse =
      SendTaskAsyncResponse.newBuilder().setTaskId(TaskId.newBuilder().setId("test").build()).build();
  private final AbortTaskResponse abortTaskResponse =
      AbortTaskResponse.newBuilder().setCanceledAtStage(TaskExecutionStage.EXECUTING).build();

  private final NgDelegateTaskServiceImplBase ngDelegateTaskServiceImplBase =
      mock(NgDelegateTaskServiceImplBase.class, delegatesTo(new NgDelegateTaskServiceImplBase() {
        public void sendTask(io.harness.delegate.SendTaskRequest request,
            io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskResponse> responseObserver) {
          responseObserver.onNext(sendTaskResponse);
          responseObserver.onCompleted();
        }

        public void sendTaskAsync(io.harness.delegate.SendTaskAsyncRequest request,
            io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskAsyncResponse> responseObserver) {
          responseObserver.onNext(sendTaskAsyncResponse);
          responseObserver.onCompleted();
        }

        public void abortTask(io.harness.delegate.AbortTaskRequest request,
            io.grpc.stub.StreamObserver<io.harness.delegate.AbortTaskResponse> responseObserver) {
          responseObserver.onNext(abortTaskResponse);
          responseObserver.onCompleted();
        }
      }));

  private NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub;
  private NgDelegateServiceGrpcClient ngDelegateServiceGrpcClient;
  @Inject private KryoSerializer kryoSerializer;

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

    ngDelegateServiceGrpcClient = new NgDelegateServiceGrpcClient(ngDelegateTaskServiceBlockingStub, kryoSerializer);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSendTask() {
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("appId", APP_ID);
    setupAbstractions.put("accountId", ACCOUNT_ID);
    TaskData taskData = TaskData.builder()
                            .async(true)
                            .taskType("HTTP")
                            .timeout(TimeUnit.MINUTES.toMillis(1))
                            .parameters(new Object[] {HttpTaskParameters.builder().url("criteria").build()})
                            .build();

    SendTaskResponse taskResponse = ngDelegateServiceGrpcClient.sendTask(ACCOUNT_ID, setupAbstractions, taskData);
    assertThat(taskResponse).isNotNull();
    assertThat(taskResponse).isEqualTo(sendTaskResponse);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSendTaskAsync() {
    SendTaskAsyncRequest sendTaskAsyncRequest = SendTaskAsyncRequest.newBuilder().build();
    SendTaskAsyncResponse taskResponse = ngDelegateServiceGrpcClient.sendTaskAsync(sendTaskAsyncRequest);
    assertThat(taskResponse).isNotNull();
    assertThat(taskResponse).isEqualTo(sendTaskAsyncResponse);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testAbortTask() {
    AbortTaskRequest abortTaskRequest = AbortTaskRequest.newBuilder().build();
    AbortTaskResponse taskResponse = ngDelegateServiceGrpcClient.abortTask(abortTaskRequest);
    assertThat(taskResponse).isNotNull();
    assertThat(taskResponse).isEqualTo(abortTaskResponse);
  }
}
