package migrations.all;

import software.wings.beans.infrastructure.CloudFormationRollbackConfig.CloudFormationRollbackConfigKeys;

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
