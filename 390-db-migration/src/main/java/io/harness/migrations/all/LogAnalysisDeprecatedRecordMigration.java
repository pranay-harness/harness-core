package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.verification.CVConfiguration;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateResults;
@Slf4j
public class LogAnalysisDeprecatedRecordMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<CVConfiguration> iterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority).fetch())) {
      for (CVConfiguration cvConfiguration : iterator) {
        log.info("migrating for {}", cvConfiguration.getUuid());
        final UpdateResults updateResults =
            wingsPersistence.update(wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority)
                                        .filter("cvConfigId", cvConfiguration.getUuid())
                                        .field("deprecated")
                                        .doesNotExist(),
                wingsPersistence.createUpdateOperations(LogMLAnalysisRecord.class).set("deprecated", false));
        log.info("updated {} records", updateResults.getUpdatedCount());
      }
    }

    log.info("migration done...");
  }
}
