package io.harness.perpetualtask.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.grpc.stub.StreamObserver;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.HeartbeatRequest;
import io.harness.perpetualtask.HeartbeatResponse;
import io.harness.perpetualtask.PerpetualTaskAssignDetails;
import io.harness.perpetualtask.PerpetualTaskContextRequest;
import io.harness.perpetualtask.PerpetualTaskContextResponse;
import io.harness.perpetualtask.PerpetualTaskListRequest;
import io.harness.perpetualtask.PerpetualTaskListResponse;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskState;

import java.util.List;

@Singleton
public class PerpetualTaskServiceGrpc
    extends io.harness.perpetualtask.PerpetualTaskServiceGrpc.PerpetualTaskServiceImplBase {
  @Inject private PerpetualTaskService perpetualTaskService;

  @Override
  public void perpetualTaskList(
      PerpetualTaskListRequest request, StreamObserver<PerpetualTaskListResponse> responseObserver) {
    List<PerpetualTaskAssignDetails> perpetualTaskAssignDetails =
        perpetualTaskService.listAssignedTasks(request.getDelegateId().getId());
    PerpetualTaskListResponse response =
        PerpetualTaskListResponse.newBuilder().addAllPerpetualTaskAssignDetails(perpetualTaskAssignDetails).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void perpetualTaskContext(
      PerpetualTaskContextRequest request, StreamObserver<PerpetualTaskContextResponse> responseObserver) {
    responseObserver.onNext(
        PerpetualTaskContextResponse.newBuilder()
            .setPerpetualTaskContext(perpetualTaskService.perpetualTaskContext(request.getPerpetualTaskId().getId()))
            .build());
    responseObserver.onCompleted();
  }

  @Override
  public void heartbeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
    String taskId = request.getId();

    PerpetualTaskResponse perpetualTaskResponse =
        PerpetualTaskResponse.builder()
            .responseMessage(request.getResponseMessage())
            .responseCode(request.getResponseCode())
            .perpetualTaskState(PerpetualTaskState.valueOf(request.getTaskState()))
            .build();
    long heartbeatMillis = HTimestamps.toInstant(request.getHeartbeatTimestamp()).toEpochMilli();
    perpetualTaskService.triggerCallback(taskId, heartbeatMillis, perpetualTaskResponse);
    responseObserver.onNext(HeartbeatResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
