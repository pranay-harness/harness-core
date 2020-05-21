package io.harness.perpetualtask.internal;

import com.google.inject.Inject;

import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.dl.WingsPersistence;

import java.util.List;
import java.util.Optional;

@Slf4j
public class PerpetualTaskRecordDao {
  private final WingsPersistence persistence;

  @Inject
  public PerpetualTaskRecordDao(WingsPersistence persistence) {
    this.persistence = persistence;
  }

  public void appointDelegate(String taskId, String delegateId, long lastContextUpdated) {
    Query<PerpetualTaskRecord> query =
        persistence.createQuery(PerpetualTaskRecord.class).filter(PerpetualTaskRecordKeys.uuid, taskId);
    UpdateOperations<PerpetualTaskRecord> updateOperations =
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.delegateId, delegateId)
            .set(PerpetualTaskRecordKeys.client_context_last_updated, lastContextUpdated);
    persistence.update(query, updateOperations);
  }

  public void setTaskState(String taskId, String state) {
    persistence.updateField(PerpetualTaskRecord.class, taskId, PerpetualTaskRecordKeys.state, state);
  }

  public boolean resetDelegateIdForTask(String accountId, String taskId) {
    Query<PerpetualTaskRecord> query = persistence.createQuery(PerpetualTaskRecord.class)
                                           .filter(PerpetualTaskRecordKeys.accountId, accountId)
                                           .filter(PerpetualTaskRecordKeys.uuid, taskId);
    UpdateOperations<PerpetualTaskRecord> updateOperations =
        persistence.createUpdateOperations(PerpetualTaskRecord.class).set(PerpetualTaskRecordKeys.delegateId, "");
    UpdateResults update = persistence.update(query, updateOperations);
    return update.getUpdatedCount() > 0;
  }

  public String save(PerpetualTaskRecord record) {
    return persistence.save(record);
  }

  public boolean remove(String accountId, String taskId) {
    Query<PerpetualTaskRecord> query = persistence.createQuery(PerpetualTaskRecord.class)
                                           .field(PerpetualTaskRecordKeys.accountId)
                                           .equal(accountId)
                                           .field(PerpetualTaskRecordKeys.uuid)
                                           .equal(taskId);
    return persistence.delete(query);
  }

  public List<PerpetualTaskRecord> listAssignedTasks(String delegateId, String accountId) {
    return persistence.createQuery(PerpetualTaskRecord.class)
        .field(PerpetualTaskRecordKeys.accountId)
        .equal(accountId)
        .field(PerpetualTaskRecordKeys.delegateId)
        .equal(delegateId)
        .project(PerpetualTaskRecordKeys.uuid, true)
        .project(PerpetualTaskRecordKeys.client_context_last_updated, true)
        .asList();
  }

  public PerpetualTaskRecord getTask(String taskId) {
    return persistence.createQuery(PerpetualTaskRecord.class).field(PerpetualTaskRecordKeys.uuid).equal(taskId).get();
  }

  public Optional<PerpetualTaskRecord> getExistingPerpetualTask(
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

  public boolean saveHeartbeat(PerpetualTaskRecord task, long heartbeatMillis) {
    UpdateOperations<PerpetualTaskRecord> taskUpdateOperations =
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.lastHeartbeat, heartbeatMillis);
    UpdateResults update = persistence.update(task, taskUpdateOperations);
    return update.getUpdatedCount() > 0;
  }
}
