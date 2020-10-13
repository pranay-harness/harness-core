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
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Singleton
@Slf4j
public class PerpetualTaskServiceGrpc
    extends io.harness.perpetualtask.PerpetualTaskServiceGrpc.PerpetualTaskServiceImplBase {
  @Inject private PerpetualTaskService perpetualTaskService;

  @Override
  public void perpetualTaskList(
      PerpetualTaskListRequest request, StreamObserver<PerpetualTaskListResponse> responseObserver) {
    logger.info("perpetualTaskList invoked");
    Instant start = Instant.now();
    List<PerpetualTaskAssignDetails> perpetualTaskAssignDetails =
        perpetualTaskService.listAssignedTasks(request.getDelegateId().getId());
    PerpetualTaskListResponse response =
        PerpetualTaskListResponse.newBuilder().addAllPerpetualTaskAssignDetails(perpetualTaskAssignDetails).build();
    Instant end = Instant.now();
    Duration duration = Duration.between(start, end);
    logger.info("perpetualTaskList duration:{}", duration);
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
    PerpetualTaskResponse perpetualTaskResponse = PerpetualTaskResponse.builder()
                                                      .responseMessage(request.getResponseMessage())
                                                      .responseCode(request.getResponseCode())
                                                      .build();
    long heartbeatMillis = HTimestamps.toInstant(request.getHeartbeatTimestamp()).toEpochMilli();
    perpetualTaskService.triggerCallback(request.getId(), heartbeatMillis, perpetualTaskResponse);
    responseObserver.onNext(HeartbeatResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
