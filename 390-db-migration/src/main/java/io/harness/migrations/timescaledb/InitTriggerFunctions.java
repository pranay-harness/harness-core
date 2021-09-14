/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.timescaledb;

public class InitTriggerFunctions extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/trigger_functions.sql";
  }
}
