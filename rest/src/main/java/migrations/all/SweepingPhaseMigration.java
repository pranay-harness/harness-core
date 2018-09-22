package migrations.all;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SweepingOutput;
import software.wings.dl.WingsPersistence;

public class SweepingPhaseMigration implements Migration {
  private static Logger logger = LoggerFactory.getLogger(SweepingPhaseMigration.class);
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<SweepingOutput> iterator =
             new HIterator<SweepingOutput>(wingsPersistence.createQuery(SweepingOutput.class)
                                               .field(SweepingOutput.PHASE_EXECUTION_ID_KEY)
                                               .doesNotExist()
                                               .fetch())) {
      while (iterator.hasNext()) {
        SweepingOutput sweepingOutput = iterator.next();
        final UpdateOperations<SweepingOutput> updateOperations =
            wingsPersistence.createUpdateOperations(SweepingOutput.class)
                .set(SweepingOutput.PHASE_EXECUTION_ID_KEY, generateUuid());
        wingsPersistence.update(sweepingOutput, updateOperations);
      }
    }
  }
}