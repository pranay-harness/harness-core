package migrations.all;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutput.SweepingOutputKeys;
import io.harness.persistence.HIterator;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;

public class SweepingPhaseMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<SweepingOutput> iterator =
             new HIterator<SweepingOutput>(wingsPersistence.createQuery(SweepingOutput.class)
                                               .field(SweepingOutputKeys.phaseExecutionId)
                                               .doesNotExist()
                                               .fetch())) {
      while (iterator.hasNext()) {
        SweepingOutput sweepingOutput = iterator.next();
        final UpdateOperations<SweepingOutput> updateOperations =
            wingsPersistence.createUpdateOperations(SweepingOutput.class)
                .set(SweepingOutputKeys.phaseExecutionId, generateUuid());
        wingsPersistence.update(sweepingOutput, updateOperations);
      }
    }
  }
}