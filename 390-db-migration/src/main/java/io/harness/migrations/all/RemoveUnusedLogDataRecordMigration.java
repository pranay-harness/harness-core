package io.harness.migrations.all;

import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveUnusedLogDataRecordMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Deleting all records without validUntil");
    final boolean deleted = wingsPersistence.delete(
        wingsPersistence.createQuery(LogDataRecord.class).field(LogDataRecordKeys.validUntil).doesNotExist());
    log.info("migration done. result {}", deleted);
  }
}
