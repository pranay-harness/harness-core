package io.harness.grpc.ng.manager;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;

import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AbortTaskRequest;
import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.AccountId;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub;
import io.harness.delegate.SendTaskAsyncRequest;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.WaitNotifyEngine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.HttpStateExecutionData;
import software.wings.service.intfc.DelegateService;

import java.io.IOException;
import java.util.HashMap;

public class DelegateTaskGrpcServerTest extends WingsBaseTest {
  @Rule public GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();
  private static final String HTTP_URL_200 = "http://httpstat.us/200";

  private NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub;
  @Inject private KryoSerializer kryoSerializer;
  @Mock private DelegateService delegateService;
  @Mock private WaitNotifyEngine waitNotifyEngine;

  @Before
  public void doSetup() throws IOException {
    String serverName = InProcessServerBuilder.generateName();

    grpcCleanupRule.register(
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new DelegateTaskGrpcServer(delegateService, kryoSerializer, waitNotifyEngine))
            .build()
            .start());

    ngDelegateTaskServiceBlockingStub = NgDelegateTaskServiceGrpc.newBlockingStub(
        grpcCleanupRule.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testAbortTask() {
    String taskId = generateUuid();
    AbortTaskResponse abortTaskResponse = ngDelegateTaskServiceBlockingStub.abortTask(
        AbortTaskRequest.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build());
    assertThat(abortTaskResponse).isNotNull();
    assertThat(abortTaskResponse.getCanceledAtStage()).isEqualTo(TaskExecutionStage.EXECUTING);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testSendAsyncTask() {
    String asyncTaskId = generateUuid();
    SendTaskAsyncRequest sendTaskAsyncRequest = buildSendTaskAsyncRequest();
    when(delegateService.queueTask(any())).thenReturn(asyncTaskId);
    SendTaskAsyncResponse sendTaskAsyncResponse = ngDelegateTaskServiceBlockingStub.sendTaskAsync(sendTaskAsyncRequest);
    assertThat(sendTaskAsyncResponse).isNotNull();
    assertThat(sendTaskAsyncResponse.getTaskId()).isNotNull();
    assertThat(sendTaskAsyncResponse.getTaskId().getId()).isEqualTo(asyncTaskId);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testSendTask() throws InterruptedException {
    SendTaskRequest sendTaskRequest = buildSendTaskRequest();
    when(delegateService.executeTask(any())).thenReturn(HttpStateExecutionData.builder().build());
    SendTaskResponse sendTaskResponse = ngDelegateTaskServiceBlockingStub.sendTask(sendTaskRequest);
    assertThat(sendTaskResponse).isNotNull();
    assertThat(sendTaskResponse.getTaskId()).isNotNull();
  }

  private SendTaskRequest buildSendTaskRequest() {
    TaskDetails taskDetails = buildTaskDetails();
    return SendTaskRequest.newBuilder()
        .setAccountId(AccountId.newBuilder().setId(generateUuid()).build())
        .setSetupAbstractions(
            TaskSetupAbstractions.newBuilder().putAllValues(ImmutableMap.of("accountId", generateUuid())).build())
        .setDetails(taskDetails)
        .build();
  }

  private SendTaskAsyncRequest buildSendTaskAsyncRequest() {
    TaskDetails taskDetails = buildTaskDetails();
    return SendTaskAsyncRequest.newBuilder()
        .setAccountId(AccountId.newBuilder().setId(generateUuid()).build())
        .setSetupAbstractions(
            TaskSetupAbstractions.newBuilder().putAllValues(ImmutableMap.of("accountId", generateUuid())).build())
        .setDetails(taskDetails)
        .build();
  }

  private TaskDetails buildTaskDetails() {
    return TaskDetails.newBuilder()
        .setType(TaskType.newBuilder().setType("TYPE").build())
        .setKryoParameters(ByteString.copyFrom(
            kryoSerializer.asDeflatedBytes(HttpTaskParameters.builder().method("GET").url(HTTP_URL_200).build())))
        .setExecutionTimeout(Duration.newBuilder().setSeconds(3).setNanos(100).build())
        .setExpressionFunctorToken(200)
        .putAllExpressions(new HashMap<>())
        .build();
  }
}
