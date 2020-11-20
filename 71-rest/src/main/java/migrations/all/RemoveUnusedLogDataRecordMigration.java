package migrations.all;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;

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
