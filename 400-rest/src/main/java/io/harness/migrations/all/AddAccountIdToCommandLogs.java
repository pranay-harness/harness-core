package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Log.LogKeys;

@TargetModule(Module._390_DB_MIGRATION)
public class AddAccountIdToCommandLogs extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "commandLogs";
  }

  @Override
  protected String getFieldName() {
    return LogKeys.accountId;
  }
}
