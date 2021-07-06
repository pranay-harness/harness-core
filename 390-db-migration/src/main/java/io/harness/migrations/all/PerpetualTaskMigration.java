package io.harness.migrations.all;

import io.harness.migrations.Migration;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;

public class PerpetualTaskMigration implements Migration {
  @Inject private HPersistence persistence;

  @Override
  public void migrate() {
    persistence.update(persistence.createQuery(PerpetualTaskRecord.class)
                           .filter(PerpetualTaskRecordKeys.state, PerpetualTaskState.NO_ELIGIBLE_DELEGATES),
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_UNASSIGNED));
    persistence.update(persistence.createQuery(PerpetualTaskRecord.class)
                           .filter(PerpetualTaskRecordKeys.state, PerpetualTaskState.NO_DELEGATE_AVAILABLE),
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_UNASSIGNED));
    persistence.update(persistence.createQuery(PerpetualTaskRecord.class)
                           .filter(PerpetualTaskRecordKeys.state, PerpetualTaskState.NO_DELEGATE_INSTALLED),
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_UNASSIGNED));
    persistence.update(persistence.createQuery(PerpetualTaskRecord.class)
                           .filter(PerpetualTaskRecordKeys.state, PerpetualTaskState.NO_ELIGIBLE_DELEGATES),
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_UNASSIGNED));

    persistence.update(persistence.createQuery(PerpetualTaskRecord.class)
                           .filter(PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_RUN_FAILED),
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_ASSIGNED));
    persistence.update(persistence.createQuery(PerpetualTaskRecord.class)
                           .filter(PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_RUN_FAILED),
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.state, PerpetualTaskState.TASK_ASSIGNED));
  }
}
