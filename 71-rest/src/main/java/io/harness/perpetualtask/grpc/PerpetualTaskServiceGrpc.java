package io.harness.perpetualtask.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;

import io.grpc.stub.StreamObserver;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.DelegateId;
import io.harness.perpetualtask.HeartbeatRequest;
import io.harness.perpetualtask.HeartbeatResponse;
import io.harness.perpetualtask.PerpetualTaskContext;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskIdList;
import io.harness.perpetualtask.PerpetualTaskParams;
import io.harness.perpetualtask.PerpetualTaskRecord;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class PerpetualTaskServiceGrpc
    extends io.harness.perpetualtask.PerpetualTaskServiceGrpc.PerpetualTaskServiceImplBase {
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private Map<PerpetualTaskType, PerpetualTaskServiceClient> clientMap;

  @Override
  public void listTaskIds(DelegateId request, StreamObserver<PerpetualTaskIdList> responseObserver) {
    List<String> taskIds = perpetualTaskService.listTaskIds(request.getId());
    List<PerpetualTaskId> perpetualTaskIds =
        taskIds.stream().map(taskId -> PerpetualTaskId.newBuilder().setId(taskId).build()).collect(Collectors.toList());
    PerpetualTaskIdList taskIdList = PerpetualTaskIdList.newBuilder().addAllTaskIds(perpetualTaskIds).build();
    responseObserver.onNext(taskIdList);
    responseObserver.onCompleted();
  }

  @Override
  public void getTaskContext(PerpetualTaskId request, StreamObserver<PerpetualTaskContext> responseObserver) {
    String taskId = request.getId();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskService.getTask(taskId);
    PerpetualTaskServiceClient client = clientMap.get(perpetualTaskRecord.getPerpetualTaskType());
    Message perpetualTaskParams = client.getTaskParams(perpetualTaskRecord.getClientContext());

    PerpetualTaskParams params =
        PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(perpetualTaskParams)).build();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromSeconds(perpetualTaskRecord.getIntervalSeconds()))
                                         .setTimeout(Durations.fromMillis(perpetualTaskRecord.getTimeoutMillis()))
                                         .build();

    PerpetualTaskContext perpetualTaskContext =
        PerpetualTaskContext.newBuilder()
            .setTaskParams(params)
            .setTaskSchedule(schedule)
            .setHeartbeatTimestamp(HTimestamps.fromMillis(perpetualTaskRecord.getLastHeartbeat()))
            .build();
    responseObserver.onNext(perpetualTaskContext);
    responseObserver.onCompleted();
  }

  @Override
  public void publishHeartbeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
    String taskId = request.getId();
    long heartbeatMillis = HTimestamps.toInstant(request.getHeartbeatTimestamp()).toEpochMilli();

    perpetualTaskService.updateHeartbeat(taskId, heartbeatMillis);
    responseObserver.onNext(HeartbeatResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
