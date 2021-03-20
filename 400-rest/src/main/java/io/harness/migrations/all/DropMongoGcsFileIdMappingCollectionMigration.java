package io.harness.migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Dropping the 'mongoGcsFileIdMapping' collection which is no longer needed or used.
 *
 * @author marklu on 2019-03-17
 */
@Slf4j
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class DropMongoGcsFileIdMappingCollectionMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      wingsPersistence.getCollection(DEFAULT_STORE, "mongoGcsFileIdMapping").drop();
    } catch (RuntimeException ex) {
      log.error("Drop collection error", ex);
    }
  }
}
