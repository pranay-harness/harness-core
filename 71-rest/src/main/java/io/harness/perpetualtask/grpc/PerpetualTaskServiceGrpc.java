package io.harness.perpetualtask.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.grpc.stub.StreamObserver;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.DelegateId;
import io.harness.perpetualtask.HeartbeatRequest;
import io.harness.perpetualtask.HeartbeatResponse;
import io.harness.perpetualtask.PerpetualTaskContext;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskIdList;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskState;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class PerpetualTaskServiceGrpc
    extends io.harness.perpetualtask.PerpetualTaskServiceGrpc.PerpetualTaskServiceImplBase {
  @Inject private PerpetualTaskService perpetualTaskService;

  @Override
  public void listTaskIds(DelegateId request, StreamObserver<PerpetualTaskIdList> responseObserver) {
    List<String> taskIds = perpetualTaskService.listAssignedTaskIds(request.getId());
    List<PerpetualTaskId> perpetualTaskIds =
        taskIds.stream().map(taskId -> PerpetualTaskId.newBuilder().setId(taskId).build()).collect(Collectors.toList());
    PerpetualTaskIdList taskIdList = PerpetualTaskIdList.newBuilder().addAllTaskIds(perpetualTaskIds).build();
    responseObserver.onNext(taskIdList);
    responseObserver.onCompleted();
  }

  @Override
  public void getTaskContext(PerpetualTaskId request, StreamObserver<PerpetualTaskContext> responseObserver) {
    responseObserver.onNext(perpetualTaskService.getTaskContext(request.getId()));
    responseObserver.onCompleted();
  }

  @Override
  public void publishHeartbeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
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
