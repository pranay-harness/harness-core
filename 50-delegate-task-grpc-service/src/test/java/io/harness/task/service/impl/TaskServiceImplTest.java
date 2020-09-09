package io.harness.task.service.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;

import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.AccountId;
import io.harness.delegate.ParkedTaskResultsResponse;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.grpc.DelegateServiceGrpcLiteClient;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.task.TaskServiceTest;
import io.harness.task.TaskServiceTestHelper;
import io.harness.task.converters.ResponseDataConverterRegistry;
import io.harness.task.service.GetTaskResultsRequest;
import io.harness.task.service.GetTaskResultsResponse;
import io.harness.task.service.RunParkedTaskRequest;
import io.harness.task.service.RunParkedTaskResponse;
import io.harness.task.service.TaskProgressRequest;
import io.harness.task.service.TaskProgressResponse;
import io.harness.task.service.TaskServiceGrpc;
import io.harness.task.service.TaskType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.io.IOException;

public class TaskServiceImplTest extends TaskServiceTest {
  @Rule public GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();
  @Mock private DelegateServiceGrpcLiteClient delegateServiceGrpcLiteClient;
  @Inject KryoSerializer kryoSerializer;
  @Inject ResponseDataConverterRegistry registry;

  @Inject TaskServiceTestHelper taskServiceTestHelper;

  private TaskServiceGrpc.TaskServiceBlockingStub taskServiceBlockingStub;
  private Server testInProcessServer;
  private AccountId accountId;
  private TaskId taskId;
  private String driverId;

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
    driverId = "driverId";
  }

  @After
  public void doCleanup() {
    testInProcessServer.shutdown();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldRunParkedTask() {
    when(delegateServiceGrpcLiteClient.runParkedTask(eq(accountId), eq(taskId)))
        .thenReturn(io.harness.delegate.RunParkedTaskResponse.newBuilder().setTaskId(taskId).build())
        .thenThrow(new IllegalArgumentException());
    RunParkedTaskResponse runParkedTaskResponse = taskServiceBlockingStub.runParkedTask(
        RunParkedTaskRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build());
    assertThat(runParkedTaskResponse).isEqualTo(RunParkedTaskResponse.newBuilder().setTaskId(taskId).build());

    assertThatThrownBy(()
                           -> taskServiceBlockingStub.runParkedTask(
                               RunParkedTaskRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build()))
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
        .isEqualTo(TaskProgressResponse.newBuilder().setCurrentlyAtStage(TaskExecutionStage.EXECUTING).build());

    assertThatThrownBy(()
                           -> taskServiceBlockingStub.taskProgress(
                               TaskProgressRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build()))
        .isInstanceOf(io.grpc.StatusRuntimeException.class);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetHTTPTaskResults() {
    when(delegateServiceGrpcLiteClient.getParkedTaskResults(accountId, taskId, driverId))
        .thenReturn(ParkedTaskResultsResponse.newBuilder()
                        .setHaveResults(true)
                        .setKryoResultsData(ByteString.copyFrom(taskServiceTestHelper.getDeflatedHttpResponseData()))
                        .build())
        .thenThrow(new IllegalArgumentException());
    GetTaskResultsResponse taskResults = taskServiceBlockingStub.getTaskResults(GetTaskResultsRequest.newBuilder()
                                                                                    .setAccountId(accountId)
                                                                                    .setTaskId(taskId)
                                                                                    .setDriverId(driverId)
                                                                                    .setTaskType(TaskType.HTTP)
                                                                                    .build());

    assertThat(taskResults)
        .isEqualTo(GetTaskResultsResponse.newBuilder()
                       .setTaskId(taskId)
                       .setTaskType(TaskType.HTTP)
                       .setHaveResponseData(true)
                       .setHttpTaskResponse(taskServiceTestHelper.getHttpTaskResponse())
                       .build());

    assertThatThrownBy(()
                           -> taskServiceBlockingStub.getTaskResults(GetTaskResultsRequest.newBuilder()
                                                                         .setAccountId(accountId)
                                                                         .setTaskId(taskId)
                                                                         .setDriverId(driverId)
                                                                         .setTaskType(TaskType.HTTP)
                                                                         .build()))
        .isInstanceOf(io.grpc.StatusRuntimeException.class);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetJIRATaskResults() {
    when(delegateServiceGrpcLiteClient.getParkedTaskResults(accountId, taskId, driverId))
        .thenReturn(ParkedTaskResultsResponse.newBuilder()
                        .setHaveResults(true)
                        .setKryoResultsData(ByteString.copyFrom(taskServiceTestHelper.getDeflatedJiraResponseData()))
                        .build())
        .thenThrow(new IllegalArgumentException());
    GetTaskResultsResponse taskResults = taskServiceBlockingStub.getTaskResults(GetTaskResultsRequest.newBuilder()
                                                                                    .setAccountId(accountId)
                                                                                    .setTaskId(taskId)
                                                                                    .setDriverId(driverId)
                                                                                    .setTaskType(TaskType.JIRA)
                                                                                    .build());

    assertThat(taskResults)
        .isEqualTo(GetTaskResultsResponse.newBuilder()
                       .setTaskId(taskId)
                       .setTaskType(TaskType.JIRA)
                       .setHaveResponseData(true)
                       .setJiraTaskResponse(taskServiceTestHelper.getJiraTaskResponse())
                       .build());

    assertThatThrownBy(()
                           -> taskServiceBlockingStub.getTaskResults(GetTaskResultsRequest.newBuilder()
                                                                         .setAccountId(accountId)
                                                                         .setTaskId(taskId)
                                                                         .setDriverId(driverId)
                                                                         .setTaskType(TaskType.JIRA)
                                                                         .build()))
        .isInstanceOf(io.grpc.StatusRuntimeException.class);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetEmptyTaskResults() {
    when(delegateServiceGrpcLiteClient.getParkedTaskResults(accountId, taskId, driverId))
        .thenReturn(ParkedTaskResultsResponse.newBuilder().setHaveResults(false).build());
    GetTaskResultsResponse taskResults = taskServiceBlockingStub.getTaskResults(GetTaskResultsRequest.newBuilder()
                                                                                    .setAccountId(accountId)
                                                                                    .setTaskId(taskId)
                                                                                    .setDriverId(driverId)
                                                                                    .setTaskType(TaskType.JIRA)
                                                                                    .build());

    assertThat(taskResults)
        .isEqualTo(GetTaskResultsResponse.newBuilder()
                       .setTaskId(taskId)
                       .setTaskType(TaskType.JIRA)
                       .setHaveResponseData(false)
                       .build());
  }
}