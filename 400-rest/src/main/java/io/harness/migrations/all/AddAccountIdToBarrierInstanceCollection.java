package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.BarrierInstance.BarrierInstanceKeys;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class AddAccountIdToBarrierInstanceCollection extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "barrierInstances";
  }

  @Override
  protected String getFieldName() {
    return BarrierInstanceKeys.accountId;
  }
}
