package io.harness.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.util.Timestamps;

import io.grpc.stub.StreamObserver;
import io.harness.delegate.CancelTaskRequest;
import io.harness.delegate.CancelTaskResponse;
import io.harness.delegate.CreatePerpetualTaskRequest;
import io.harness.delegate.CreatePerpetualTaskResponse;
import io.harness.delegate.DelegateServiceGrpc.DelegateServiceImplBase;
import io.harness.delegate.DeletePerpetualTaskRequest;
import io.harness.delegate.DeletePerpetualTaskResponse;
import io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest;
import io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse;
import io.harness.delegate.ResetPerpetualTaskRequest;
import io.harness.delegate.ResetPerpetualTaskResponse;
import io.harness.delegate.SubmitTaskRequest;
import io.harness.delegate.SubmitTaskResponse;
import io.harness.delegate.TaskProgressRequest;
import io.harness.delegate.TaskProgressResponse;
import io.harness.delegate.TaskProgressUpdatesRequest;
import io.harness.delegate.TaskProgressUpdatesResponse;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;

@Singleton
public class DelegateServiceGrpc extends DelegateServiceImplBase {
  private PerpetualTaskService perpetualTaskService;

  @Inject
  public DelegateServiceGrpc(PerpetualTaskService perpetualTaskService) {
    this.perpetualTaskService = perpetualTaskService;
  }

  @Override
  public void submitTask(SubmitTaskRequest request, StreamObserver<SubmitTaskResponse> responseObserver) {
    responseObserver.onNext(SubmitTaskResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void cancelTask(CancelTaskRequest request, StreamObserver<CancelTaskResponse> responseObserver) {
    responseObserver.onNext(CancelTaskResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void taskProgress(TaskProgressRequest request, StreamObserver<TaskProgressResponse> responseObserver) {
    responseObserver.onNext(TaskProgressResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void taskProgressUpdates(
      TaskProgressUpdatesRequest request, StreamObserver<TaskProgressUpdatesResponse> responseObserver) {
    /* Some loop should be used here, around onNext, in order to generate a stream of events */
    responseObserver.onNext(TaskProgressUpdatesResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void registerPerpetualTaskClientEntrypoint(RegisterPerpetualTaskClientEntrypointRequest request,
      StreamObserver<RegisterPerpetualTaskClientEntrypointResponse> responseObserver) {
    responseObserver.onNext(RegisterPerpetualTaskClientEntrypointResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void createPerpetualTask(
      CreatePerpetualTaskRequest request, StreamObserver<CreatePerpetualTaskResponse> responseObserver) {
    PerpetualTaskType type = PerpetualTaskType.valueOf(request.getType());
    String accountId = request.getAccountId().getId();

    PerpetualTaskClientContext context = new PerpetualTaskClientContext(request.getContext().getTaskClientParamsMap());
    if (request.getContext().getLastContextUpdated() != null) {
      context.setLastContextUpdated(Timestamps.toMillis(request.getContext().getLastContextUpdated()));
    }

    String perpetualTaskId =
        perpetualTaskService.createTask(type, accountId, context, request.getSchedule(), request.getAllowDuplicate());

    responseObserver.onNext(CreatePerpetualTaskResponse.newBuilder()
                                .setPerpetualTaskId(PerpetualTaskId.newBuilder().setId(perpetualTaskId).build())
                                .build());
    responseObserver.onCompleted();
  }

  @Override
  public void deletePerpetualTask(
      DeletePerpetualTaskRequest request, StreamObserver<DeletePerpetualTaskResponse> responseObserver) {
    perpetualTaskService.deleteTask(request.getAccountId().getId(), request.getPerpetualTaskId().getId());

    responseObserver.onNext(DeletePerpetualTaskResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void resetPerpetualTask(
      ResetPerpetualTaskRequest request, StreamObserver<ResetPerpetualTaskResponse> responseObserver) {
    perpetualTaskService.resetTask(request.getAccountId().getId(), request.getPerpetualTaskId().getId());

    responseObserver.onNext(ResetPerpetualTaskResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
