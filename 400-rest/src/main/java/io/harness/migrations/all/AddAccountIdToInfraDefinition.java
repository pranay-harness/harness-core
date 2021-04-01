package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;

@TargetModule(HarnessModule._390_DB_MIGRATION)
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
