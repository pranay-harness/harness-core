package io.harness.grpc;

import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.AccountId;
import io.harness.delegate.DelegateServiceGrpc;
import io.harness.delegate.ExecuteParkedTaskRequest;
import io.harness.delegate.ExecuteParkedTaskResponse;
import io.harness.delegate.FetchParkedTaskStatusRequest;
import io.harness.delegate.FetchParkedTaskStatusResponse;
import io.harness.delegate.SendTaskProgressRequest;
import io.harness.delegate.SendTaskProgressResponse;
import io.harness.delegate.SendTaskStatusRequest;
import io.harness.delegate.SendTaskStatusResponse;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskProgressRequest;
import io.harness.delegate.TaskProgressResponse;
import io.harness.delegate.TaskResponseData;
import io.harness.exception.DelegateServiceDriverException;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;

public class DelegateServiceGrpcLiteClient {
  private final DelegateServiceGrpc.DelegateServiceBlockingStub delegateServiceBlockingStub;

  @Inject
  public DelegateServiceGrpcLiteClient(DelegateServiceGrpc.DelegateServiceBlockingStub delegateServiceBlockingStub) {
    this.delegateServiceBlockingStub = delegateServiceBlockingStub;
  }

  public ExecuteParkedTaskResponse executeParkedTask(AccountId accountId, TaskId taskId) {
    try {
      return delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
          .executeParkedTask(ExecuteParkedTaskRequest.newBuilder().setTaskId(taskId).setAccountId(accountId).build());
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while executing parked task.", ex);
    }
  }

  public FetchParkedTaskStatusResponse fetchParkedTaskStatus(
      AccountId accountId, TaskId taskId, DelegateCallbackToken delegateCallbackToken) {
    try {
      return delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
          .fetchParkedTaskStatus(FetchParkedTaskStatusRequest.newBuilder()
                                     .setAccountId(accountId)
                                     .setTaskId(taskId)
                                     .setCallbackToken(delegateCallbackToken)
                                     .build());
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred fetching parked task results.", ex);
    }
  }

  public TaskExecutionStage taskProgress(AccountId accountId, TaskId taskId) {
    try {
      TaskProgressResponse response =
          delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
              .taskProgress(TaskProgressRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build());

      return response.getCurrentlyAtStage();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while checking task progress.", ex);
    }
  }

  public boolean sendTaskStatus(
      AccountId accountId, TaskId taskId, DelegateCallbackToken delegateCallbackToken, byte[] responseData) {
    try {
      SendTaskStatusResponse response =
          delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
              .sendTaskStatus(
                  SendTaskStatusRequest.newBuilder()
                      .setAccountId(accountId)
                      .setTaskId(taskId)
                      .setCallbackToken(delegateCallbackToken)
                      .setTaskResponseData(
                          TaskResponseData.newBuilder().setKryoResultsData(ByteString.copyFrom(responseData)).build())
                      .build());

      return response.getSuccess();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while checking task progress.", ex);
    }
  }

  public boolean sendTaskProgressUpdate(
      AccountId accountId, TaskId taskId, DelegateCallbackToken delegateCallbackToken, byte[] responseData) {
    try {
      SendTaskProgressResponse response =
          delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
              .sendTaskProgress(
                  SendTaskProgressRequest.newBuilder()
                      .setAccountId(accountId)
                      .setTaskId(taskId)
                      .setCallbackToken(delegateCallbackToken)
                      .setTaskResponseData(
                          TaskResponseData.newBuilder().setKryoResultsData(ByteString.copyFrom(responseData)).build())
                      .build());

      return response.getSuccess();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while sending task progress update.", ex);
    }
  }
}
