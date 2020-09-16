package io.harness.perpetualtask.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.DelegateId;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.HeartbeatRequest;
import io.harness.perpetualtask.PerpetualTaskAssignDetails;
import io.harness.perpetualtask.PerpetualTaskContextRequest;
import io.harness.perpetualtask.PerpetualTaskExecutionContext;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskListRequest;
import io.harness.perpetualtask.PerpetualTaskListResponse;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskServiceGrpc.PerpetualTaskServiceBlockingStub;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class PerpetualTaskServiceGrpcClient {
  private final PerpetualTaskServiceBlockingStub serviceBlockingStub;

  @Inject
  public PerpetualTaskServiceGrpcClient(PerpetualTaskServiceBlockingStub perpetualTaskServiceBlockingStub) {
    serviceBlockingStub = perpetualTaskServiceBlockingStub;
  }

  public List<PerpetualTaskAssignDetails> perpetualTaskList(String delegateId) {
    PerpetualTaskListResponse response =
        serviceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
            .perpetualTaskList(PerpetualTaskListRequest.newBuilder()
                                   .setDelegateId(DelegateId.newBuilder().setId(delegateId).build())
                                   .build());
    return response.getPerpetualTaskAssignDetailsList();
  }

  public PerpetualTaskExecutionContext perpetualTaskContext(PerpetualTaskId taskId) {
    return serviceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
        .perpetualTaskContext(PerpetualTaskContextRequest.newBuilder().setPerpetualTaskId(taskId).build())
        .getPerpetualTaskContext();
  }

  public void heartbeat(PerpetualTaskId taskId, Instant taskStartTime, PerpetualTaskResponse perpetualTaskResponse) {
    serviceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
        .heartbeat(HeartbeatRequest.newBuilder()
                       .setId(taskId.getId())
                       .setHeartbeatTimestamp(HTimestamps.fromInstant(taskStartTime))
                       .setResponseCode(perpetualTaskResponse.getResponseCode())
                       .setResponseMessage(perpetualTaskResponse.getResponseMessage())
                       .build());
  }
}
