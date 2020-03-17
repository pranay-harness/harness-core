package io.harness.perpetualtask.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.DelegateId;
import io.harness.perpetualtask.HeartbeatRequest;
import io.harness.perpetualtask.PerpetualTaskContext;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskIdList;
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

  public List<PerpetualTaskId> listTaskIds(String delegateId) {
    PerpetualTaskIdList perpetualTaskIdList = serviceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                                                  .listTaskIds(DelegateId.newBuilder().setId(delegateId).build());
    return perpetualTaskIdList.getTaskIdsList();
  }

  public PerpetualTaskContext getTaskContext(PerpetualTaskId taskId) {
    return serviceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).getTaskContext(taskId);
  }

  public void publishHeartbeat(
      PerpetualTaskId taskId, Instant taskStartTime, PerpetualTaskResponse perpetualTaskResponse) {
    serviceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
        .publishHeartbeat(HeartbeatRequest.newBuilder()
                              .setId(taskId.getId())
                              .setHeartbeatTimestamp(HTimestamps.fromInstant(taskStartTime))
                              .setResponseCode(perpetualTaskResponse.getResponseCode())
                              .setTaskState(perpetualTaskResponse.getPerpetualTaskState().name())
                              .setResponseMessage(perpetualTaskResponse.getResponseMessage())
                              .build());
  }
}
