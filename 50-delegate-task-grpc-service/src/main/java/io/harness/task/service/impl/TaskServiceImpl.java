package io.harness.task.service.impl;

import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.grpc.stub.StreamObserver;
import io.harness.delegate.ParkedTaskResultsResponse;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.exception.InvalidArgumentsException;
import io.harness.grpc.DelegateServiceGrpcLiteClient;
import io.harness.serializer.KryoSerializer;
import io.harness.task.converters.ResponseDataConverterRegistry;
import io.harness.task.service.GetTaskResultsRequest;
import io.harness.task.service.GetTaskResultsResponse;
import io.harness.task.service.HTTPTaskResponse;
import io.harness.task.service.JiraTaskResponse;
import io.harness.task.service.RunParkedTaskRequest;
import io.harness.task.service.RunParkedTaskResponse;
import io.harness.task.service.TaskProgressRequest;
import io.harness.task.service.TaskProgressResponse;
import io.harness.task.service.TaskServiceGrpc;
import io.harness.task.service.TaskType;
import io.harness.tasks.ResponseData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskServiceImpl extends TaskServiceGrpc.TaskServiceImplBase {
  private final DelegateServiceGrpcLiteClient delegateServiceGrpcLiteClient;
  private final KryoSerializer kryoSerializer;
  private final ResponseDataConverterRegistry responseDataConverterRegistry;

  @Inject
  public TaskServiceImpl(DelegateServiceGrpcLiteClient delegateServiceGrpcLiteClient, KryoSerializer kryoSerializer,
      ResponseDataConverterRegistry responseDataConverterRegistry) {
    this.delegateServiceGrpcLiteClient = delegateServiceGrpcLiteClient;
    this.kryoSerializer = kryoSerializer;
    this.responseDataConverterRegistry = responseDataConverterRegistry;
  }

  @Override
  public void runParkedTask(RunParkedTaskRequest request, StreamObserver<RunParkedTaskResponse> responseObserver) {
    try {
      delegateServiceGrpcLiteClient.runParkedTask(request.getAccountId(), request.getTaskId());
      responseObserver.onNext(RunParkedTaskResponse.newBuilder().setTaskId(request.getTaskId()).build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing runParkedTask request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void taskProgress(TaskProgressRequest request, StreamObserver<TaskProgressResponse> responseObserver) {
    try {
      TaskExecutionStage taskExecutionStage =
          delegateServiceGrpcLiteClient.taskProgress(request.getAccountId(), request.getTaskId());
      responseObserver.onNext(TaskProgressResponse.newBuilder().setCurrentlyAtStage(taskExecutionStage).build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing taskProgress request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void getTaskResults(GetTaskResultsRequest request, StreamObserver<GetTaskResultsResponse> responseObserver) {
    try {
      ParkedTaskResultsResponse parkedTaskResults = delegateServiceGrpcLiteClient.getParkedTaskResults(
          request.getAccountId(), request.getTaskId(), request.getDriverId());
      if (parkedTaskResults.getHaveResults()) {
        responseObserver.onNext(buildGetTaskResultsResponse(
            request.getTaskId(), request.getTaskType(), parkedTaskResults.getKryoResultsData().toByteArray()));
      } else {
        responseObserver.onNext(GetTaskResultsResponse.newBuilder()
                                    .setTaskId(request.getTaskId())
                                    .setTaskType(request.getTaskType())
                                    .setHaveResponseData(false)
                                    .build());
      }
      responseObserver.onCompleted();
    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing getTaskResults request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  private GetTaskResultsResponse buildGetTaskResultsResponse(
      TaskId taskId, TaskType taskType, byte[] responseDataByteArray) {
    GetTaskResultsResponse.Builder builder = GetTaskResultsResponse.newBuilder();
    builder.setTaskId(taskId).setTaskType(taskType).setHaveResponseData(true);

    ResponseData responseData = (ResponseData) kryoSerializer.asInflatedObject(responseDataByteArray);
    switch (taskType) {
      case JIRA:
        JiraTaskResponse jiraTaskResponse =
            responseDataConverterRegistry.<JiraTaskResponse>obtain(taskType).convert(responseData);
        return builder.setJiraTaskResponse(jiraTaskResponse).build();
      case HTTP:
        HTTPTaskResponse httpTaskResponse =
            responseDataConverterRegistry.<HTTPTaskResponse>obtain(taskType).convert(responseData);
        return builder.setHttpTaskResponse(httpTaskResponse).build();

      default:
        unhandled(taskType);
        throw new InvalidArgumentsException(format("Can't execute task with type:%s", taskType));
    }
  }
}
