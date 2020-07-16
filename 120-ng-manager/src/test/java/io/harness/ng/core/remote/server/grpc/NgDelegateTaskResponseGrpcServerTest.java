package io.harness.ng.core.remote.server.grpc;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceBlockingStub;
import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.remote.server.grpc.perpetualtask.RemotePerpetualTaskServiceClientManager;
import io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsRequest;
import io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse;
import io.harness.perpetualtask.ObtainPerpetualTaskValidationDetailsRequest;
import io.harness.perpetualtask.ObtainPerpetualTaskValidationDetailsResponse;
import io.harness.perpetualtask.PerpetualTaskExecutionResponse;
import io.harness.perpetualtask.RemotePerpetualTaskClientContext;
import io.harness.perpetualtask.ReportPerpetualTaskStateChangeRequest;
import io.harness.perpetualtask.ReportPerpetualTaskStateChangeResponse;
import io.harness.perpetualtask.example.SamplePerpetualTaskParams;
import io.harness.perpetualtask.remote.RemotePerpetualTaskType;
import io.harness.perpetualtask.remote.ValidationTaskDetails;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.WaitNotifyEngine;
import org.jetbrains.annotations.NotNull;
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
  @Mock private RemotePerpetualTaskServiceClientManager pTaskServiceClientManager;

  @Before
  public void doSetup() throws IOException {
    String serverName = InProcessServerBuilder.generateName();

    grpcCleanupRule.register(InProcessServerBuilder.forName(serverName)
                                 .directExecutor()
                                 .addService(new NgDelegateTaskResponseGrpcServer(
                                     waitNotifyEngine, kryoSerializer, pTaskServiceClientManager))
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

    SendTaskResultResponse sendTaskResultResponse =
        ngDelegateTaskServiceBlockingStub.sendTaskResult(SendTaskResultRequest.newBuilder().setTaskId(taskId).build());

    assertThat(sendTaskResultResponse).isNotNull();
    assertThat(sendTaskResultResponse.getAcknowledgement()).isTrue();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void obtainPerpetualTaskValidationDetails() {
    final ValidationTaskDetails validationTaskDetails =
        ValidationTaskDetails.builder()
            .accountId("accountId")
            .taskData(TaskData.builder()
                          .taskType("http")
                          .parameters(new Object[] {HttpTaskParameters.builder().build()})
                          .build())
            .build();

    doReturn(validationTaskDetails)
        .when(pTaskServiceClientManager)
        .getValidationTask(anyString(), any(RemotePerpetualTaskClientContext.class), anyString());
    final ObtainPerpetualTaskValidationDetailsRequest request = ObtainPerpetualTaskValidationDetailsRequest.newBuilder()
                                                                    .setAccountId("accountid")
                                                                    .setTaskType(getTaskType())
                                                                    .setContext(getRemoteTaskContext())
                                                                    .build();
    final ObtainPerpetualTaskValidationDetailsResponse response =
        ngDelegateTaskServiceBlockingStub.obtainPerpetualTaskValidationDetails(request);
    assertThat(response.getDetails().getType().getType()).isEqualTo("http");
    assertThat(response.getDetails().getKryoParameters()).isNotNull();
  }

  private String getTaskType() {
    return RemotePerpetualTaskType.REMOTE_SAMPLE.getTaskType();
  }

  @NotNull
  private RemotePerpetualTaskClientContext getRemoteTaskContext() {
    return RemotePerpetualTaskClientContext.newBuilder()
        .putAllTaskClientParams(ImmutableMap.of("country", "dummy-country"))
        .build();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void obtainPerpetualTaskExecutionParams() {
    doReturn(SamplePerpetualTaskParams.newBuilder().build())
        .when(pTaskServiceClientManager)
        .getTaskParams(anyString(), any(RemotePerpetualTaskClientContext.class));
    final ObtainPerpetualTaskExecutionParamsRequest request = ObtainPerpetualTaskExecutionParamsRequest.newBuilder()
                                                                  .setTaskType(getTaskType())
                                                                  .setContext(getRemoteTaskContext())
                                                                  .build();
    final ObtainPerpetualTaskExecutionParamsResponse response =
        ngDelegateTaskServiceBlockingStub.obtainPerpetualTaskExecutionParams(request);
    assertThat(response.getCustomizedParams()).isNotNull();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void reportPerpetualTaskStateChange() {
    doNothing()
        .when(pTaskServiceClientManager)
        .reportPerpetualTaskStateChange(anyString(), anyString(), any(PerpetualTaskExecutionResponse.class),
            any(PerpetualTaskExecutionResponse.class));
    final ReportPerpetualTaskStateChangeRequest request =
        ReportPerpetualTaskStateChangeRequest.newBuilder()
            .setTaskType(getTaskType())
            .setPerpetualTaskId("id")
            .setOldTaskResponse(PerpetualTaskExecutionResponse.newBuilder().build())
            .setNewTaskResponse(PerpetualTaskExecutionResponse.newBuilder().build())
            .build();

    final ReportPerpetualTaskStateChangeResponse response =
        ngDelegateTaskServiceBlockingStub.reportPerpetualTaskStateChange(request);
    assertThat(response).isNotNull();
  }
}
