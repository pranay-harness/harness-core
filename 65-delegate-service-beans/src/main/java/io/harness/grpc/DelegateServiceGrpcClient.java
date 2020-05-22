package io.harness.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.CancelTaskRequest;
import io.harness.delegate.CancelTaskResponse;
import io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import io.harness.delegate.SubmitTaskRequest;
import io.harness.delegate.SubmitTaskResponse;
import io.harness.delegate.TaskCapabilities;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskProgressRequest;
import io.harness.delegate.TaskProgressResponse;
import io.harness.delegate.TaskProgressUpdatesRequest;
import io.harness.delegate.TaskProgressUpdatesResponse;
import io.harness.delegate.TaskSetupAbstractions;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
@Slf4j
public class DelegateServiceGrpcClient {
  private final DelegateServiceBlockingStub delegateServiceBlockingStub;

  @Inject
  public DelegateServiceGrpcClient(DelegateServiceBlockingStub delegateServiceBlockingStub) {
    this.delegateServiceBlockingStub = delegateServiceBlockingStub;
  }

  public TaskId submitTask(
      TaskSetupAbstractions taskSetupAbstractions, TaskDetails taskDetails, TaskCapabilities taskCapabilities) {
    SubmitTaskResponse response = delegateServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                                      .submitTask(SubmitTaskRequest.newBuilder()
                                                      .setSetupAbstractions(taskSetupAbstractions)
                                                      .setDetails(taskDetails)
                                                      .setCapabilities(taskCapabilities)
                                                      .build());

    return response.getTaskId();
  }

  public TaskExecutionStage cancelTask(TaskId taskId) {
    CancelTaskResponse response = delegateServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                                      .cancelTask(CancelTaskRequest.newBuilder().setTaskId(taskId).build());

    return response.getCanceledAtStage();
  }

  public TaskExecutionStage taskProgress(TaskId taskId) {
    TaskProgressResponse response = delegateServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                                        .taskProgress(TaskProgressRequest.newBuilder().setTaskId(taskId).build());

    return response.getCurrentlyAtStage();
  }

  public List<TaskExecutionStage> taskProgressUpdate(TaskId taskId) {
    Iterator<TaskProgressUpdatesResponse> responseIterator =
        delegateServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
            .taskProgressUpdates(TaskProgressUpdatesRequest.newBuilder().setTaskId(taskId).build());

    Iterable<TaskProgressUpdatesResponse> iterable = () -> responseIterator;
    return StreamSupport.stream(iterable.spliterator(), false)
        .map(TaskProgressUpdatesResponse::getCurrentlyAtStage)
        .collect(Collectors.toList());
  }
}
