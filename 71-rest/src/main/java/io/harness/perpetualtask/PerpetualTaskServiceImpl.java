package io.harness.perpetualtask;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;

import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecordDao;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Singleton
@Slf4j
public class PerpetualTaskServiceImpl implements PerpetualTaskService {
  private final PerpetualTaskRecordDao perpetualTaskRecordDao;
  private final PerpetualTaskServiceClientRegistry clientRegistry;

  @Inject
  public PerpetualTaskServiceImpl(
      PerpetualTaskRecordDao perpetualTaskRecordDao, PerpetualTaskServiceClientRegistry clientRegistry) {
    this.perpetualTaskRecordDao = perpetualTaskRecordDao;
    this.clientRegistry = clientRegistry;
  }

  @Override
  public String createTask(PerpetualTaskType perpetualTaskType, String accountId,
      PerpetualTaskClientContext clientContext, PerpetualTaskSchedule schedule, boolean allowDuplicate) {
    if (!allowDuplicate) {
      Optional<PerpetualTaskRecord> perpetualTaskMaybe =
          perpetualTaskRecordDao.getExistingPerpetualTask(accountId, perpetualTaskType, clientContext);
      if (perpetualTaskMaybe.isPresent()) {
        PerpetualTaskRecord perpetualTaskRecord = perpetualTaskMaybe.get();
        logger.info("Perpetual task exist {} ", perpetualTaskRecord.getUuid());
        return perpetualTaskRecord.getUuid();
      }
    }

    PerpetualTaskRecord record = PerpetualTaskRecord.builder()
                                     .accountId(accountId)
                                     .perpetualTaskType(perpetualTaskType)
                                     .clientContext(clientContext)
                                     .timeoutMillis(Durations.toMillis(schedule.getTimeout()))
                                     .intervalSeconds(schedule.getInterval().getSeconds())
                                     .lastHeartbeat(Instant.now().toEpochMilli())
                                     .delegateId("")
                                     .build();

    return perpetualTaskRecordDao.save(record);
  }

  @Override
  public boolean deleteTask(String accountId, String taskId) {
    return perpetualTaskRecordDao.remove(accountId, taskId);
  }

  @Override
  public List<String> listAssignedTaskIds(String delegateId) {
    return perpetualTaskRecordDao.listAssignedTaskIds(delegateId);
  }

  @Override
  public PerpetualTaskContext getTaskContext(String taskId) {
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecordDao.getTask(taskId);

    PerpetualTaskParams params = getTaskParams(perpetualTaskRecord);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromSeconds(perpetualTaskRecord.getIntervalSeconds()))
                                         .setTimeout(Durations.fromMillis(perpetualTaskRecord.getTimeoutMillis()))
                                         .build();

    return PerpetualTaskContext.newBuilder()
        .setTaskParams(params)
        .setTaskSchedule(schedule)
        .setHeartbeatTimestamp(HTimestamps.fromMillis(perpetualTaskRecord.getLastHeartbeat()))
        .build();
  }

  private PerpetualTaskParams getTaskParams(PerpetualTaskRecord perpetualTaskRecord) {
    PerpetualTaskServiceClient client = clientRegistry.getClient(perpetualTaskRecord.getPerpetualTaskType());
    Message perpetualTaskParams = client.getTaskParams(perpetualTaskRecord.getClientContext());
    return PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(perpetualTaskParams)).build();
  }

  public boolean updateHeartbeat(String taskId, long heartbeatMillis) {
    PerpetualTaskRecord taskRecord = perpetualTaskRecordDao.getTask(taskId);
    if (null == taskRecord || taskRecord.getLastHeartbeat() > heartbeatMillis) {
      return false;
    }
    return perpetualTaskRecordDao.saveHeartbeat(taskRecord, heartbeatMillis);
  }
}
