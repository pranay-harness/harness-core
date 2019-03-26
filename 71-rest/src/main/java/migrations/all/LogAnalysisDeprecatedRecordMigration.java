package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.verification.CVConfiguration;

public class LogAnalysisDeprecatedRecordMigration implements Migration {
  private static Logger logger = LoggerFactory.getLogger(LogAnalysisDeprecatedRecordMigration.class);
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