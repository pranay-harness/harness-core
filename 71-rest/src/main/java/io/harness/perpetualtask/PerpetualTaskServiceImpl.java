package io.harness.perpetualtask;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.util.Durations;

import io.harness.perpetualtask.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.dl.WingsPersistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class PerpetualTaskServiceImpl implements PerpetualTaskService {
  @Inject private WingsPersistence persistence;

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
}
