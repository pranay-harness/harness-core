package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.infrastructure.CloudFormationRollbackConfig.CloudFormationRollbackConfigKeys;

@TargetModule(Module._390_DB_MIGRATION)
public class AddAccountIdToCloudFormationRollBackConfig extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "cloudFormationRollbackConfig";
  }

  @Override
  protected String getFieldName() {
    return CloudFormationRollbackConfigKeys.accountId;
  }
}
