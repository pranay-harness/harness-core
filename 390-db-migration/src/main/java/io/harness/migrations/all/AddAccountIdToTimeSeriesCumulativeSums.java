package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
public class AddAccountIdToTimeSeriesCumulativeSums extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesCumulativeSums";
  }

  @Override
  protected String getFieldName() {
    return "accountId";
  }
}
