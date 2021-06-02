package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;

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
