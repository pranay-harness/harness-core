package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.ResourceConstraintInstance.ResourceConstraintInstanceKeys;

@TargetModule(HarnessModule._390_DB_MIGRATION)
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
