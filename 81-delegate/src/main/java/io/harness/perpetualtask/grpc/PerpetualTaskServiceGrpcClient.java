package io.harness.perpetualtask.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.DelegateId;
import io.harness.perpetualtask.HeartbeatRequest;
import io.harness.perpetualtask.PerpetualTaskContext;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskIdList;
import io.harness.perpetualtask.PerpetualTaskServiceGrpc.PerpetualTaskServiceBlockingStub;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Singleton
@Slf4j
public class PerpetualTaskServiceGrpcClient {
  private final PerpetualTaskServiceBlockingStub serviceBlockingStub;

  @Inject
  public PerpetualTaskServiceGrpcClient(PerpetualTaskServiceBlockingStub perpetualTaskServiceBlockingStub) {
    serviceBlockingStub = perpetualTaskServiceBlockingStub;
  }

  public List<PerpetualTaskId> listTaskIds(String delegateId) {
    PerpetualTaskIdList perpetualTaskIdList =
        serviceBlockingStub.listTaskIds(DelegateId.newBuilder().setId(delegateId).build());
    return perpetualTaskIdList.getTaskIdsList();
  }

  public PerpetualTaskContext getTaskContext(PerpetualTaskId taskId) {
    return serviceBlockingStub.getTaskContext(taskId);
  }

  public void publishHeartbeat(PerpetualTaskId taskId) {
    serviceBlockingStub.publishHeartbeat(HeartbeatRequest.newBuilder()
                                             .setId(taskId.getId())
                                             .setHeartbeatTimestamp(HTimestamps.fromInstant(Instant.now()))
                                             .build());
  }
}
