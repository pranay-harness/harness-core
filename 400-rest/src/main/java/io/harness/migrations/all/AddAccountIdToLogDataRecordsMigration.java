package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._390_DB_MIGRATION)
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
