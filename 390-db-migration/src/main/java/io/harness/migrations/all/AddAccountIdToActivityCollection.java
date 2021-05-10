package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;

import software.wings.beans.Activity.ActivityKeys;

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
