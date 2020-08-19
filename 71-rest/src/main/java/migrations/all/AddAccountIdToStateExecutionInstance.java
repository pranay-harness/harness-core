package migrations.all;

import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;

public class AddAccountIdToStateExecutionInstance extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "stateExecutionInstances";
  }

  @Override
  protected String getFieldName() {
    return StateExecutionInstanceKeys.accountId;
  }
}