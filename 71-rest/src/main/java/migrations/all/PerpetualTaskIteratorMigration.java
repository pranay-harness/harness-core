package migrations.all;

import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import migrations.Migration;

public class PerpetualTaskIteratorMigration implements Migration {
  @Inject private HPersistence persistence;

  @Override
  public void migrate() {
    persistence.update(
        persistence.createQuery(PerpetualTaskRecord.class).filter(PerpetualTaskRecordKeys.rebalanceIteration, null),
        persistence.createUpdateOperations(PerpetualTaskRecord.class)
            .set(PerpetualTaskRecordKeys.rebalanceIteration, 0));
  }
}
