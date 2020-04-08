package migrations.all;

import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;

@Slf4j
public class AddAccountIdToLogDataRecordsMigration extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "logDataRecords";
  }

  @Override
  protected String getFieldName() {
    return LogDataRecordKeys.accountId;
  }
}
