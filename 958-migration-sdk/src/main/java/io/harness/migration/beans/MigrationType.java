/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
