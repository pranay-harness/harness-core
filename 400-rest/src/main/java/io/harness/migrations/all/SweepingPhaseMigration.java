package io.harness.migrations.all;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.SweepingOutputInstanceKeys;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import org.mongodb.morphia.query.UpdateOperations;

@TargetModule(Module._390_DB_MIGRATION)
public class SweepingPhaseMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<SweepingOutputInstance> iterator =
             new HIterator<SweepingOutputInstance>(wingsPersistence.createQuery(SweepingOutputInstance.class)
                                                       .field(SweepingOutputInstanceKeys.phaseExecutionId)
                                                       .doesNotExist()
                                                       .fetch())) {
      for (SweepingOutputInstance sweepingOutputInstance : iterator) {
        final UpdateOperations<SweepingOutputInstance> updateOperations =
            wingsPersistence.createUpdateOperations(SweepingOutputInstance.class)
                .set(SweepingOutputInstanceKeys.phaseExecutionId, generateUuid());
        wingsPersistence.update(sweepingOutputInstance, updateOperations);
      }
    }
  }
}
