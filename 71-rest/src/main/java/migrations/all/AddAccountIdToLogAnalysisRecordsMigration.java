package migrations.all;

import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;

@Slf4j
public class AddAccountIdToLogAnalysisRecordsMigration extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "logAnalysisRecords";
  }

  @Override
  protected String getFieldName() {
    return LogMLAnalysisRecordKeys.accountId;
  }
}
