package io.harness.perpetualtask;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;

import io.grpc.Context;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.grpc.utils.HTimestamps;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecordDao;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;

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
    try (AutoLogContext ignore0 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      if (!allowDuplicate) {
        Optional<PerpetualTaskRecord> perpetualTaskMaybe =
            perpetualTaskRecordDao.getExistingPerpetualTask(accountId, perpetualTaskType, clientContext);
        if (perpetualTaskMaybe.isPresent()) {
          PerpetualTaskRecord perpetualTaskRecord = perpetualTaskMaybe.get();
          logger.info("Perpetual task with id={} exists.", perpetualTaskRecord.getUuid());
          return perpetualTaskRecord.getUuid();
        }
      }

      PerpetualTaskRecord record = PerpetualTaskRecord.builder()
                                       .accountId(accountId)
                                       .perpetualTaskType(perpetualTaskType)
                                       .clientContext(clientContext)
                                       .timeoutMillis(Durations.toMillis(schedule.getTimeout()))
                                       .intervalSeconds(schedule.getInterval().getSeconds())
                                       .delegateId("")
                                       .build();
      String taskId = perpetualTaskRecordDao.save(record);
      try (AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId, OVERRIDE_ERROR)) {
        logger.info("Created a perpetual task with id={}.", taskId);
      }
      return taskId;
    }
  }

  @Override
  public boolean resetTask(String accountId, String taskId) {
    try (AutoLogContext ignore0 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId, OVERRIDE_ERROR)) {
      return perpetualTaskRecordDao.resetDelegateId(accountId, taskId);
    }
  }

  @Override
  public boolean deleteTask(String accountId, String taskId) {
    try (AutoLogContext ignore0 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId, OVERRIDE_ERROR)) {
      boolean hasDeleted = perpetualTaskRecordDao.remove(accountId, taskId);
      if (hasDeleted) {
        logger.info("Deleted the perpetual task with id={}.", taskId);
      }
      return hasDeleted;
    }
  }

  @Override
  public List<String> listAssignedTaskIds(String delegateId) {
    String accountId = DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY.get(Context.current());
    logger.debug("Account id: {}", accountId);
    return perpetualTaskRecordDao.listAssignedTaskIds(delegateId, accountId);
  }

  @Override
  public PerpetualTaskRecord getTaskRecord(String taskId) {
    return perpetualTaskRecordDao.getTask(taskId);
  }

  @Override
  public PerpetualTaskType getPerpetualTaskType(String taskId) {
    PerpetualTaskRecord perpetualTaskRecord = getTaskRecord(taskId);
    return perpetualTaskRecord.getPerpetualTaskType();
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

  @Override
  public boolean updateHeartbeat(String taskId, long heartbeatMillis) {
    PerpetualTaskRecord taskRecord = perpetualTaskRecordDao.getTask(taskId);
    if (null == taskRecord || taskRecord.getLastHeartbeat() > heartbeatMillis) {
      return false;
    }
    return perpetualTaskRecordDao.saveHeartbeat(taskRecord, heartbeatMillis);
  }
}
