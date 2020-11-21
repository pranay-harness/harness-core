package io.harness.task.service.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.SANJA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.callback.DelegateCallbackToken;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AccountId;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.grpc.DelegateServiceGrpcLiteClient;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.task.TaskServiceTestBase;
import io.harness.task.TaskServiceTestHelper;
import io.harness.task.converters.ResponseDataConverterRegistry;
import io.harness.task.service.ExecuteParkedTaskRequest;
import io.harness.task.service.ExecuteParkedTaskResponse;
import io.harness.task.service.FetchParkedTaskStatusRequest;
import io.harness.task.service.FetchParkedTaskStatusResponse;
import io.harness.task.service.SendTaskProgressRequest;
import io.harness.task.service.SendTaskProgressResponse;
import io.harness.task.service.SendTaskStatusRequest;
import io.harness.task.service.SendTaskStatusResponse;
import io.harness.task.service.TaskProgressRequest;
import io.harness.task.service.TaskProgressResponse;
import io.harness.task.service.TaskServiceGrpc;
import io.harness.task.service.TaskStatusData;
import io.harness.task.service.TaskType;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class TaskServiceImplTest extends TaskServiceTestBase {
  @Rule public GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();
  @Mock private DelegateServiceGrpcLiteClient delegateServiceGrpcLiteClient;
  @Inject KryoSerializer kryoSerializer;
  @Inject ResponseDataConverterRegistry registry;

  @Inject TaskServiceTestHelper taskServiceTestHelper;

  private TaskServiceGrpc.TaskServiceBlockingStub taskServiceBlockingStub;
  private Server testInProcessServer;
  private AccountId accountId;
  private TaskId taskId;
  private DelegateCallbackToken delegateCallbackToken;

  @Before
  public void doSetup() throws IOException {
    TaskServiceTestHelper.registerConverters(registry);
    TaskServiceImpl taskService = new TaskServiceImpl(delegateServiceGrpcLiteClient, kryoSerializer, registry);

    String serverName = InProcessServerBuilder.generateName();
    testInProcessServer = grpcCleanupRule.register(
        InProcessServerBuilder.forName(serverName).directExecutor().addService(taskService).build().start());
    taskServiceBlockingStub = TaskServiceGrpc.newBlockingStub(
        grpcCleanupRule.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

    accountId = AccountId.newBuilder().setId("accountId").build();
    taskId = TaskId.newBuilder().setId("taskId").build();
    delegateCallbackToken = DelegateCallbackToken.newBuilder().setToken("driverId").build();
  }

  @After
  public void doCleanup() {
    testInProcessServer.shutdown();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldExecuteParkedTask() {
    when(delegateServiceGrpcLiteClient.executeParkedTask(eq(accountId), eq(taskId)))
        .thenReturn(io.harness.delegate.ExecuteParkedTaskResponse.newBuilder().setTaskId(taskId).build())
        .thenThrow(new IllegalArgumentException());
    ExecuteParkedTaskResponse executeParkedTaskResponse = taskServiceBlockingStub.executeParkedTask(
        ExecuteParkedTaskRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build());
    assertThat(executeParkedTaskResponse).isEqualTo(ExecuteParkedTaskResponse.newBuilder().setTaskId(taskId).build());

    assertThatThrownBy(()
                           -> taskServiceBlockingStub.executeParkedTask(
                               ExecuteParkedTaskRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build()))
        .isInstanceOf(io.grpc.StatusRuntimeException.class);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetTaskProgress() {
    when(delegateServiceGrpcLiteClient.taskProgress(eq(accountId), eq(taskId)))
        .thenReturn(TaskExecutionStage.EXECUTING)
        .thenThrow(new IllegalArgumentException());
    TaskProgressResponse taskProgressResponse = taskServiceBlockingStub.taskProgress(
        TaskProgressRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build());
    assertThat(taskProgressResponse)
        .isEqualTo(TaskProgressResponse.newBuilder().setCurrentStage(TaskExecutionStage.EXECUTING).build());

    assertThatThrownBy(()
                           -> taskServiceBlockingStub.taskProgress(
                               TaskProgressRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build()))
        .isInstanceOf(io.grpc.StatusRuntimeException.class);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetHTTPTaskResults() {
    when(delegateServiceGrpcLiteClient.fetchParkedTaskStatus(accountId, taskId, delegateCallbackToken))
        .thenReturn(
            io.harness.delegate.FetchParkedTaskStatusResponse.newBuilder()
                .setFetchResults(true)
                .setSerializedTaskResults(ByteString.copyFrom(taskServiceTestHelper.getDeflatedHttpResponseData()))
                .build())
        .thenThrow(new IllegalArgumentException());
    FetchParkedTaskStatusResponse taskResults =
        taskServiceBlockingStub.fetchParkedTaskStatus(FetchParkedTaskStatusRequest.newBuilder()
                                                          .setAccountId(accountId)
                                                          .setTaskId(taskId)
                                                          .setCallbackToken(delegateCallbackToken)
                                                          .setTaskType(TaskType.HTTP)
                                                          .build());

    assertThat(taskResults)
        .isEqualTo(
            FetchParkedTaskStatusResponse.newBuilder()
                .setTaskId(taskId)
                .setTaskType(TaskType.HTTP)
                .setHaveResponseData(true)
                .setHttpTaskResponse(taskServiceTestHelper.getHttpTaskResponse())
                .setSerializedTaskResults(ByteString.copyFrom(taskServiceTestHelper.getDeflatedHttpResponseData()))
                .build());

    assertThatThrownBy(()
                           -> taskServiceBlockingStub.fetchParkedTaskStatus(FetchParkedTaskStatusRequest.newBuilder()
                                                                                .setAccountId(accountId)
                                                                                .setTaskId(taskId)
                                                                                .setCallbackToken(delegateCallbackToken)
                                                                                .setTaskType(TaskType.HTTP)
                                                                                .build()))
        .isInstanceOf(io.grpc.StatusRuntimeException.class);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetJIRATaskResults() {
    when(delegateServiceGrpcLiteClient.fetchParkedTaskStatus(accountId, taskId, delegateCallbackToken))
        .thenReturn(
            io.harness.delegate.FetchParkedTaskStatusResponse.newBuilder()
                .setFetchResults(true)
                .setSerializedTaskResults(ByteString.copyFrom(taskServiceTestHelper.getDeflatedJiraResponseData()))
                .build())
        .thenThrow(new IllegalArgumentException());
    FetchParkedTaskStatusResponse taskResults =
        taskServiceBlockingStub.fetchParkedTaskStatus(FetchParkedTaskStatusRequest.newBuilder()
                                                          .setAccountId(accountId)
                                                          .setTaskId(taskId)
                                                          .setCallbackToken(delegateCallbackToken)
                                                          .setTaskType(TaskType.JIRA)
                                                          .build());

    assertThat(taskResults)
        .isEqualTo(
            FetchParkedTaskStatusResponse.newBuilder()
                .setTaskId(taskId)
                .setTaskType(TaskType.JIRA)
                .setHaveResponseData(true)
                .setJiraTaskResponse(taskServiceTestHelper.getJiraTaskResponse())
                .setSerializedTaskResults(ByteString.copyFrom(taskServiceTestHelper.getDeflatedJiraResponseData()))
                .build());

    assertThatThrownBy(()
                           -> taskServiceBlockingStub.fetchParkedTaskStatus(FetchParkedTaskStatusRequest.newBuilder()
                                                                                .setAccountId(accountId)
                                                                                .setTaskId(taskId)
                                                                                .setCallbackToken(delegateCallbackToken)
                                                                                .setTaskType(TaskType.JIRA)
                                                                                .build()))
        .isInstanceOf(io.grpc.StatusRuntimeException.class);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetEmptyTaskResults() {
    when(delegateServiceGrpcLiteClient.fetchParkedTaskStatus(accountId, taskId, delegateCallbackToken))
        .thenReturn(io.harness.delegate.FetchParkedTaskStatusResponse.newBuilder().setFetchResults(false).build());
    FetchParkedTaskStatusResponse taskResults =
        taskServiceBlockingStub.fetchParkedTaskStatus(FetchParkedTaskStatusRequest.newBuilder()
                                                          .setAccountId(accountId)
                                                          .setTaskId(taskId)
                                                          .setCallbackToken(delegateCallbackToken)
                                                          .setTaskType(TaskType.JIRA)
                                                          .build());

    assertThat(taskResults)
        .isEqualTo(FetchParkedTaskStatusResponse.newBuilder()
                       .setTaskId(taskId)
                       .setTaskType(TaskType.JIRA)
                       .setHaveResponseData(false)
                       .build());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldSendTaskStatus() {
    when(delegateServiceGrpcLiteClient.sendTaskStatus(eq(accountId), eq(taskId), eq(delegateCallbackToken),
             eq(taskServiceTestHelper.getDeflatedStepStatusTaskResponseData())))
        .thenReturn(true)
        .thenThrow(new IllegalArgumentException());
    SendTaskStatusResponse sendTaskStatusResponse =
        taskServiceBlockingStub.sendTaskStatus(SendTaskStatusRequest.newBuilder()
                                                   .setAccountId(accountId)
                                                   .setTaskId(taskId)
                                                   .setCallbackToken(delegateCallbackToken)
                                                   .setTaskStatusData(taskServiceTestHelper.getTaskResponseData())
                                                   .build());
    assertThat(sendTaskStatusResponse).isEqualTo(SendTaskStatusResponse.newBuilder().setSuccess(true).build());

    SendTaskStatusResponse sendTaskStatusEmptyResponse =
        taskServiceBlockingStub.sendTaskStatus(SendTaskStatusRequest.newBuilder()
                                                   .setAccountId(accountId)
                                                   .setTaskId(taskId)
                                                   .setCallbackToken(delegateCallbackToken)
                                                   .setTaskStatusData(TaskStatusData.newBuilder().build())
                                                   .build());
    assertThat(sendTaskStatusEmptyResponse).isEqualTo(SendTaskStatusResponse.newBuilder().setSuccess(false).build());

    assertThatThrownBy(()
                           -> taskServiceBlockingStub.sendTaskStatus(
                               SendTaskStatusRequest.newBuilder()
                                   .setAccountId(accountId)
                                   .setTaskId(taskId)
                                   .setCallbackToken(delegateCallbackToken)
                                   .setTaskStatusData(taskServiceTestHelper.getTaskResponseData())
                                   .build()))
        .isInstanceOf(io.grpc.StatusRuntimeException.class);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldSendTaskProgressSuccess() {
    when(delegateServiceGrpcLiteClient.sendTaskProgressUpdate(eq(accountId), eq(taskId), eq(delegateCallbackToken),
             eq(taskServiceTestHelper.getTaskProgressResponseData().getKryoResultsData().toByteArray())))
        .thenReturn(true);
    SendTaskProgressResponse sendTaskProgressResponse = taskServiceBlockingStub.sendTaskProgress(
        SendTaskProgressRequest.newBuilder()
            .setAccountId(accountId)
            .setTaskId(taskId)
            .setCallbackToken(delegateCallbackToken)
            .setTaskResponseData(taskServiceTestHelper.getTaskProgressResponseData())
            .build());

    assertThat(sendTaskProgressResponse).isEqualTo(SendTaskProgressResponse.newBuilder().setSuccess(true).build());
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldSendTaskProgressException() {
    when(delegateServiceGrpcLiteClient.sendTaskProgressUpdate(eq(accountId), eq(taskId), eq(delegateCallbackToken),
             eq(taskServiceTestHelper.getTaskProgressResponseData().getKryoResultsData().toByteArray())))
        .thenThrow(new IllegalArgumentException());

    assertThatThrownBy(()
                           -> taskServiceBlockingStub.sendTaskProgress(
                               SendTaskProgressRequest.newBuilder()
                                   .setAccountId(accountId)
                                   .setTaskId(taskId)
                                   .setCallbackToken(delegateCallbackToken)
                                   .setTaskResponseData(taskServiceTestHelper.getTaskProgressResponseData())
                                   .build()))
        .isInstanceOf(io.grpc.StatusRuntimeException.class);
  }
}
