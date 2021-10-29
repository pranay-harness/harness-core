package io.harness.migrations.timescaledb;

public class AddCDNGEntitiesColumns extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_cdng_entities_columns.sql";
  }
}
