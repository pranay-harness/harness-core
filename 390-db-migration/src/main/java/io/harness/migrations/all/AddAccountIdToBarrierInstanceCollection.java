package io.harness.migrations.all;

import software.wings.beans.BarrierInstance.BarrierInstanceKeys;

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
