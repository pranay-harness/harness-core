package io.harness.perpetualtask;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.util.Durations;

import io.grpc.Context;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.perpetualtask.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.dl.WingsPersistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class PerpetualTaskServiceImpl implements PerpetualTaskService {
  private final WingsPersistence persistence;

  @Inject
  public PerpetualTaskServiceImpl(WingsPersistence persistence) {
    this.persistence = persistence;
  }

  @Override
  public String createTask(PerpetualTaskType perpetualTaskType, String accountId,
      PerpetualTaskClientContext clientContext, PerpetualTaskSchedule schedule, boolean allowDuplicate) {
    if (!allowDuplicate) {
      Optional<PerpetualTaskRecord> perpetualTaskMaybe =
          getExistingPerpetualTask(accountId, perpetualTaskType, clientContext);
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

    return persistence.save(record);
  }

  private Optional<PerpetualTaskRecord> getExistingPerpetualTask(
      String accountId, PerpetualTaskType perpetualTaskType, PerpetualTaskClientContext clientContext) {
    PerpetualTaskRecord perpetualTaskRecord = persistence.createQuery(PerpetualTaskRecord.class)
                                                  .field(PerpetualTaskRecordKeys.accountId)
                                                  .equal(accountId)
                                                  .field(PerpetualTaskRecordKeys.perpetualTaskType)
                                                  .equal(perpetualTaskType)
                                                  .field(PerpetualTaskRecordKeys.clientContext)
                                                  .equal(clientContext)
                                                  .get();
    return Optional.ofNullable(perpetualTaskRecord);
  }

  @Override
  public boolean deleteTask(String accountId, String taskId) {
    Query<PerpetualTaskRecord> query = persistence.createQuery(PerpetualTaskRecord.class)
                                           .field(PerpetualTaskRecordKeys.accountId)
                                           .equal(accountId)
                                           .field(PerpetualTaskRecordKeys.uuid)
                                           .equal(taskId);
    return persistence.delete(query);
  }

  @Override
  public List<String> listTaskIds(String delegateId) {
    logger.info("Account id is: {}", DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY.get(Context.current()));
    List<PerpetualTaskRecord> records = persistence.createQuery(PerpetualTaskRecord.class)
                                            .field(PerpetualTaskRecordKeys.accountId)
                                            .equal(GLOBAL_ACCOUNT_ID)
                                            .field(PerpetualTaskRecordKeys.delegateId)
                                            .equal(delegateId)
                                            .asList();

    return records.stream().map(PerpetualTaskRecord::getUuid).collect(Collectors.toList());
  }

  @Override
  public PerpetualTaskRecord getTask(String taskId) {
    return persistence.createQuery(PerpetualTaskRecord.class).field(PerpetualTaskRecordKeys.uuid).equal(taskId).get();
  }

  @Override
  public boolean updateHeartbeat(String taskId, long heartbeatMillis) {
    PerpetualTaskRecord task = getTask(taskId);
    if (null == task || task.getLastHeartbeat() > heartbeatMillis) {
      return false;
    }
    UpdateOperations<PerpetualTaskRecord> taskUpdateOperations =
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.lastHeartbeat, heartbeatMillis);
    UpdateResults update = persistence.update(task, taskUpdateOperations);
    return update.getUpdatedCount() > 0;
  }
}
