package io.harness.grpc.ng.manager;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;

import io.harness.beans.DelegateTask;
import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.TaskParameters;
import io.harness.serializer.KryoSerializer;
import software.wings.service.intfc.DelegateService;

public class DelegateTaskGrpcServer extends NgDelegateTaskServiceGrpc.NgDelegateTaskServiceImplBase {
  private DelegateService delegateService;
  private KryoSerializer kryoSerializer;

  @Inject
  public DelegateTaskGrpcServer(DelegateService delegateService, KryoSerializer kryoSerializer) {
    this.delegateService = delegateService;
    this.kryoSerializer = kryoSerializer;
  }

  @Override
  public void sendTask(io.harness.delegate.SendTaskRequest request,
      io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskResponse> responseObserver) {
    TaskDetails taskDetails = request.getDetails();
    TaskParameters parameters =
        (TaskParameters) kryoSerializer.asInflatedObject(taskDetails.getKryoParameters().toByteArray());
    String taskId = generateUuid();
    DelegateTask task = DelegateTask.builder()
                            .uuid(taskId)
                            .waitId(taskId)
                            .accountId(request.getAccountId().getId())
                            .setupAbstractions(request.getSetupAbstractions().getValuesMap())
                            .data(TaskData.builder()
                                      .taskType(taskDetails.getType().getType())
                                      .parameters(new Object[] {parameters})
                                      .timeout(Durations.toMillis(taskDetails.getExecutionTimeout()))
                                      .expressionFunctorToken((int) taskDetails.getExpressionFunctorToken())
                                      .expressions(taskDetails.getExpressionsMap())
                                      .build())
                            .build();
    try {
      ResponseData responseData = delegateService.executeTask(task);
      responseObserver.onNext(SendTaskResponse.newBuilder()
                                  .setTaskId(TaskId.newBuilder().setId(taskId).build())
                                  .setResponseData(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(responseData)))
                                  .build());
      responseObserver.onCompleted();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      responseObserver.onError(ex);
    }
  }

  @Override
  public void sendTaskAsync(io.harness.delegate.SendTaskAsyncRequest request,
      io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskAsyncResponse> responseObserver) {
    SendTaskAsyncResponse sendTaskAsyncResponse =
        SendTaskAsyncResponse.newBuilder().setTaskId(request.getTaskId()).build();
    responseObserver.onNext(sendTaskAsyncResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void abortTask(io.harness.delegate.AbortTaskRequest request,
      io.grpc.stub.StreamObserver<io.harness.delegate.AbortTaskResponse> responseObserver) {
    AbortTaskResponse abortTaskResponse =
        AbortTaskResponse.newBuilder().setCanceledAtStage(TaskExecutionStage.EXECUTING).build();
    responseObserver.onNext(abortTaskResponse);
    responseObserver.onCompleted();
  }
}
