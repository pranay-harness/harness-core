package io.harness.migrations.all;

import software.wings.beans.Log.LogKeys;

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
