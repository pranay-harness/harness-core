package migrations.all;

import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;

public class AddAccountIdToInfraDefinition extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "infrastructureDefinitions";
  }

  @Override
  protected String getFieldName() {
    return InfrastructureDefinitionKeys.accountId;
  }
}
