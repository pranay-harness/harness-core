package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Activity.ActivityKeys;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class AddAccountIdToActivityCollection extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "activities";
  }

  @Override
  protected String getFieldName() {
    return ActivityKeys.accountId;
  }
}
