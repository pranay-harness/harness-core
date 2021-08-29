package io.harness.migrations.timescaledb;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CE)
public class CreateIndexOnKubernetesUtilizationData extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/createIndexOnKubernetesUtilizationData.sql";
  }
}
