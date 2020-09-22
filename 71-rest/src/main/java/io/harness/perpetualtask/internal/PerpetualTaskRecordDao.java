package io.harness.perpetualtask.internal;

import static io.harness.perpetualtask.PerpetualTaskState.TASK_ASSIGNED;

import com.google.inject.Inject;

import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.PerpetualTaskUnassignedReason;
import io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.dl.WingsPersistence;

import java.util.ArrayList;
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
            .set(PerpetualTaskRecordKeys.state, TASK_ASSIGNED)
            .unset(PerpetualTaskRecordKeys.unassignedReason)
            .unset(PerpetualTaskRecordKeys.assignerIterations)
            .set(PerpetualTaskRecordKeys.client_context_last_updated, lastContextUpdated);
    persistence.update(query, updateOperations);
  }

  public void updateTaskUnassignedReason(String taskId, PerpetualTaskUnassignedReason reason) {
    Query<PerpetualTaskRecord> query = persistence.createQuery(PerpetualTaskRecord.class)
                                           .filter(PerpetualTaskRecordKeys.uuid, taskId)
                                           .filter(PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_UNASSIGNED);
    UpdateOperations<PerpetualTaskRecord> updateOperations =
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.unassignedReason, reason);
    persistence.update(query, updateOperations);
  }

  public boolean resetDelegateIdForTask(
      String accountId, String taskId, PerpetualTaskExecutionBundle taskExecutionBundle) {
    Query<PerpetualTaskRecord> query = persistence.createQuery(PerpetualTaskRecord.class)
                                           .filter(PerpetualTaskRecordKeys.accountId, accountId)
                                           .filter(PerpetualTaskRecordKeys.uuid, taskId);

    UpdateOperations<PerpetualTaskRecord> updateOperations =
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.delegateId, "")
            .set(PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_UNASSIGNED)
            .unset(PerpetualTaskRecordKeys.unassignedReason)
            .unset(PerpetualTaskRecordKeys.assignerIterations);

    if (taskExecutionBundle != null) {
      updateOperations.set(PerpetualTaskRecordKeys.task_parameters, taskExecutionBundle.toByteArray());
    } else {
      updateOperations.unset(PerpetualTaskRecordKeys.task_parameters);
    }

    UpdateResults update = persistence.update(query, updateOperations);
    return update.getUpdatedCount() > 0;
  }

  public boolean pauseTask(String accountId, String taskId) {
    Query<PerpetualTaskRecord> query = persistence.createQuery(PerpetualTaskRecord.class)
                                           .filter(PerpetualTaskRecordKeys.accountId, accountId)
                                           .filter(PerpetualTaskRecordKeys.uuid, taskId);

    UpdateOperations<PerpetualTaskRecord> updateOperations =
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.delegateId, "")
            .set(PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_PAUSED)
            .unset(PerpetualTaskRecordKeys.assignerIterations);

    updateOperations.unset(PerpetualTaskRecordKeys.task_parameters);

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

  public boolean removeAllTasksForAccount(String accountId) {
    Query<PerpetualTaskRecord> query =
        persistence.createQuery(PerpetualTaskRecord.class).field(PerpetualTaskRecordKeys.accountId).equal(accountId);
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

  public List<PerpetualTaskRecord> listAllPerpetualTasksForAccount(String accountId) {
    List<PerpetualTaskRecord> perpetualTaskRecords = new ArrayList<>();

    Query<PerpetualTaskRecord> query =
        persistence.createQuery(PerpetualTaskRecord.class).field(PerpetualTaskRecordKeys.accountId).equal(accountId);
    try (HIterator<PerpetualTaskRecord> tasksIterator = new HIterator<>(query.fetch())) {
      while (tasksIterator.hasNext()) {
        perpetualTaskRecords.add(tasksIterator.next());
      }
    }

    return perpetualTaskRecords;
  }

  public PerpetualTaskRecord getTask(String taskId) {
    return persistence.createQuery(PerpetualTaskRecord.class).field(PerpetualTaskRecordKeys.uuid).equal(taskId).get();
  }

  public Optional<PerpetualTaskRecord> getExistingPerpetualTask(
      String accountId, String perpetualTaskType, PerpetualTaskClientContext clientContext) {
    Query<PerpetualTaskRecord> perpetualTaskRecordQuery = persistence.createQuery(PerpetualTaskRecord.class)
                                                              .field(PerpetualTaskRecordKeys.accountId)
                                                              .equal(accountId)
                                                              .field(PerpetualTaskRecordKeys.perpetualTaskType)
                                                              .equal(perpetualTaskType);

    if (clientContext.getClientParams() != null) {
      perpetualTaskRecordQuery.field(PerpetualTaskRecordKeys.client_params).equal(clientContext.getClientParams());
    }
    if (clientContext.getExecutionBundle() != null) {
      perpetualTaskRecordQuery.field(PerpetualTaskRecordKeys.task_parameters).equal(clientContext.getExecutionBundle());
    }

    return Optional.ofNullable(perpetualTaskRecordQuery.get());
  }

  public boolean saveHeartbeat(String taskId, long heartbeatMillis) {
    // TODO: make sure that the heartbeat is coming from the right assignment. There is a race
    //       that could register heartbeat comming from wrong assignment
    Query<PerpetualTaskRecord> query = persistence.createQuery(PerpetualTaskRecord.class)
                                           .filter(PerpetualTaskRecordKeys.uuid, taskId)
                                           .filter(PerpetualTaskRecordKeys.state, TASK_ASSIGNED);

    UpdateOperations<PerpetualTaskRecord> taskUpdateOperations =
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.lastHeartbeat, heartbeatMillis);
    UpdateResults update = persistence.update(query, taskUpdateOperations);
    return update.getUpdatedCount() > 0;
  }

  public void detachTaskFromDelegate(String accountId, String delegateId) {
    Query<PerpetualTaskRecord> query = persistence.createQuery(PerpetualTaskRecord.class)
                                           .filter(PerpetualTaskRecordKeys.accountId, accountId)
                                           .filter(PerpetualTaskRecordKeys.delegateId, delegateId);

    UpdateOperations<PerpetualTaskRecord> updateOperations =
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.delegateId, "")
            .set(PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_UNASSIGNED)
            .unset(PerpetualTaskRecordKeys.unassignedReason)
            .unset(PerpetualTaskRecordKeys.assignerIterations);

    persistence.update(query, updateOperations);
  }
}
