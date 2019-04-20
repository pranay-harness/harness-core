package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.verification.CVConfiguration;
@Slf4j
public class LogAnalysisDeprecatedRecordMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<CVConfiguration> iterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority).fetch())) {
      while (iterator.hasNext()) {
        CVConfiguration cvConfiguration = iterator.next();
        logger.info("migrating for {}", cvConfiguration.getUuid());
        final UpdateResults updateResults =
            wingsPersistence.update(wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority)
                                        .filter("cvConfigId", cvConfiguration.getUuid())
                                        .field("deprecated")
                                        .doesNotExist(),
                wingsPersistence.createUpdateOperations(LogMLAnalysisRecord.class).set("deprecated", false));
        logger.info("updated {} records", updateResults.getUpdatedCount());
      }
    }

    logger.info("migration done...");
  }
}