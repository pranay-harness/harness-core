package io.harness.migration.beans;

public enum MigrationType {
  MongoMigration("MongoMigration"),
  MongoBGMigration("BackgroundMongoMigration"),
  TimeScaleMigration("TimeScaleDBMigration"),
  TimeScaleBGMigration("BackgroundTimeScaleDBMigration");

  private final String migration;

  MigrationType(String migration) {
    this.migration = migration;
  }
}
