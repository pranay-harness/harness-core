package io.harness.migrations.timescaledb;

public class CreateTaglinksTables extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_taglinks_table.sql";
  }
}
