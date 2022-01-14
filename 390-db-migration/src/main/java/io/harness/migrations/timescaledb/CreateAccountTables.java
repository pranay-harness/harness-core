package io.harness.migrations.timescaledb;

public class CreateAccountTables extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/createAccountTable.sql";
  }
}
