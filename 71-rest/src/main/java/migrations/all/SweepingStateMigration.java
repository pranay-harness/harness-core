package migrations.all;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.SweepingOutputInstanceKeys;
import io.harness.persistence.HIterator;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;

public class SweepingStateMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<SweepingOutputInstance> iterator =
             new HIterator<SweepingOutputInstance>(wingsPersistence.createQuery(SweepingOutputInstance.class)
                                                       .field(SweepingOutputInstanceKeys.stateExecutionId)
                                                       .doesNotExist()
                                                       .fetch())) {
      for (SweepingOutputInstance sweepingOutputInstance : iterator) {
        final UpdateOperations<SweepingOutputInstance> updateOperations =
            wingsPersistence.createUpdateOperations(SweepingOutputInstance.class)
                .set(SweepingOutputInstanceKeys.stateExecutionId, generateUuid());
        wingsPersistence.update(sweepingOutputInstance, updateOperations);
      }
    }
  }
}
