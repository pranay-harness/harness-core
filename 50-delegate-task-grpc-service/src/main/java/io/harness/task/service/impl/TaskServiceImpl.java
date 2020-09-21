package io.harness.task.service.impl;

import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.task.stepstatus.StepStatusTaskResponseData;
import io.harness.exception.InvalidArgumentsException;
import io.harness.grpc.DelegateServiceGrpcLiteClient;
import io.harness.serializer.KryoSerializer;
import io.harness.task.converters.ResponseDataConverterRegistry;
import io.harness.task.service.ExecuteParkedTaskRequest;
import io.harness.task.service.ExecuteParkedTaskResponse;
import io.harness.task.service.FetchParkedTaskStatusRequest;
import io.harness.task.service.FetchParkedTaskStatusResponse;
import io.harness.task.service.HTTPTaskResponse;
import io.harness.task.service.JiraTaskResponse;
import io.harness.task.service.SendTaskStatusRequest;
import io.harness.task.service.SendTaskStatusResponse;
import io.harness.task.service.TaskProgressRequest;
import io.harness.task.service.TaskProgressResponse;
import io.harness.task.service.TaskServiceGrpc;
import io.harness.task.service.TaskStatusData;
import io.harness.task.service.TaskType;
import io.harness.tasks.ResponseData;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

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
  public void executeParkedTask(
      ExecuteParkedTaskRequest request, StreamObserver<ExecuteParkedTaskResponse> responseObserver) {
    logger.info("Received fetchParkedTaskStatus call, accountId:{}, taskId:{}", request.getAccountId().getId(),
        request.getTaskId().getId());
    try {
      delegateServiceGrpcLiteClient.executeParkedTask(request.getAccountId(), request.getTaskId());
      responseObserver.onNext(ExecuteParkedTaskResponse.newBuilder().setTaskId(request.getTaskId()).build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing execute parked task request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void taskProgress(TaskProgressRequest request, StreamObserver<TaskProgressResponse> responseObserver) {
    logger.info("Received fetchParkedTaskStatus call, accountId:{}, taskId:{}", request.getAccountId().getId(),
        request.getTaskId().getId());
    try {
      TaskExecutionStage taskExecutionStage =
          delegateServiceGrpcLiteClient.taskProgress(request.getAccountId(), request.getTaskId());
      responseObserver.onNext(TaskProgressResponse.newBuilder().setCurrentStage(taskExecutionStage).build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing taskProgress request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void fetchParkedTaskStatus(
      FetchParkedTaskStatusRequest request, StreamObserver<FetchParkedTaskStatusResponse> responseObserver) {
    logger.info("Received fetchParkedTaskStatus call, accountId:{}, taskId:{}, callbackToken:{}",
        request.getAccountId().getId(), request.getTaskId().getId(), request.getCallbackToken().getToken());
    try {
      io.harness.delegate.FetchParkedTaskStatusResponse fetchParkedTaskStatusResponse =
          delegateServiceGrpcLiteClient.fetchParkedTaskStatus(
              request.getAccountId(), request.getTaskId(), request.getCallbackToken());
      if (fetchParkedTaskStatusResponse.getFetchResults()) {
        responseObserver.onNext(buildFetchParkedTaskStatusResponse(
            request.getTaskId(), request.getTaskType(), fetchParkedTaskStatusResponse.getSerializedTaskResults()));
      } else {
        responseObserver.onNext(FetchParkedTaskStatusResponse.newBuilder()
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

  private FetchParkedTaskStatusResponse buildFetchParkedTaskStatusResponse(
      TaskId taskId, TaskType taskType, ByteString responseDataByteArray) {
    FetchParkedTaskStatusResponse.Builder builder = FetchParkedTaskStatusResponse.newBuilder();
    builder.setTaskId(taskId).setTaskType(taskType).setHaveResponseData(true).setSerializedTaskResults(
        responseDataByteArray);

    ResponseData responseData = (ResponseData) kryoSerializer.asInflatedObject(responseDataByteArray.toByteArray());
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

  @Override
  public void sendTaskStatus(SendTaskStatusRequest request, StreamObserver<SendTaskStatusResponse> responseObserver) {
    logger.info("Received sendTaskStatus call, accountId:{}, taskId:{}, callbackToken:{}",
        request.getAccountId().getId(), request.getTaskId().getId(), request.getCallbackToken().getToken());
    try {
      TaskStatusData taskStatusData = request.getTaskStatusData();
      if (taskStatusData.hasStepStatus()) {
        io.harness.task.service.StepStatus stepStatus = taskStatusData.getStepStatus();
        StepStatusTaskResponseData responseData =
            StepStatusTaskResponseData.builder()
                .stepStatus(io.harness.delegate.task.stepstatus.StepStatus.builder()
                                .numberOfRetries(stepStatus.getNumRetries())
                                .totalTimeTaken(Duration.ofSeconds(stepStatus.getTotalTimeTaken().getSeconds())
                                                    .plusNanos(stepStatus.getTotalTimeTaken().getNanos()))
                                .stepExecutionStatus(io.harness.delegate.task.stepstatus.StepExecutionStatus.valueOf(
                                    stepStatus.getStepExecutionStatus().name()))
                                .output(io.harness.delegate.task.stepstatus.StepMapOutput.builder()
                                            .map(stepStatus.getStepOutput().getOutputMap())
                                            .build())
                                .error(stepStatus.getErrorMessage())
                                .build())
                .build();
        boolean success = delegateServiceGrpcLiteClient.sendTaskStatus(request.getAccountId(), request.getTaskId(),
            request.getCallbackToken(), kryoSerializer.asDeflatedBytes(responseData));

        responseObserver.onNext(SendTaskStatusResponse.newBuilder().setSuccess(success).build());
      } else {
        responseObserver.onNext(SendTaskStatusResponse.newBuilder().setSuccess(false).build());
      }
      responseObserver.onCompleted();

    } catch (Exception ex) {
      logger.error("Unexpected error occurred while processing getTaskResults request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }
}
