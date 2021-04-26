package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;

import software.wings.beans.ResourceConstraintInstance.ResourceConstraintInstanceKeys;

public class AddAccountIdToResourceContraintInstanceCollection extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "resourceConstraintInstances";
  }

  @Override
  protected String getFieldName() {
    return ResourceConstraintInstanceKeys.accountId;
  }
}
