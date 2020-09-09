package io.harness.grpc;

import com.google.inject.Inject;

import io.grpc.StatusRuntimeException;
import io.harness.delegate.AccountId;
import io.harness.delegate.DelegateServiceGrpc;
import io.harness.delegate.ParkedTaskResultsRequest;
import io.harness.delegate.ParkedTaskResultsResponse;
import io.harness.delegate.RunParkedTaskRequest;
import io.harness.delegate.RunParkedTaskResponse;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskProgressRequest;
import io.harness.delegate.TaskProgressResponse;
import io.harness.exception.DelegateServiceDriverException;

import java.util.concurrent.TimeUnit;

public class DelegateServiceGrpcLiteClient {
  private final DelegateServiceGrpc.DelegateServiceBlockingStub delegateServiceBlockingStub;

  @Inject
  public DelegateServiceGrpcLiteClient(DelegateServiceGrpc.DelegateServiceBlockingStub delegateServiceBlockingStub) {
    this.delegateServiceBlockingStub = delegateServiceBlockingStub;
  }

  public RunParkedTaskResponse runParkedTask(AccountId accountId, TaskId taskId) {
    try {
      return delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
          .runParkedTask(RunParkedTaskRequest.newBuilder().setTaskId(taskId).setAccountId(accountId).build());
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while running parked task.", ex);
    }
  }

  public ParkedTaskResultsResponse getParkedTaskResults(AccountId accountId, TaskId taskId, String driverId) {
    try {
      return delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
          .parkedTaskResults(ParkedTaskResultsRequest.newBuilder()
                                 .setAccountId(accountId)
                                 .setTaskId(taskId)
                                 .setDriverId(driverId)
                                 .build());
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred getting parked task results.", ex);
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
}
