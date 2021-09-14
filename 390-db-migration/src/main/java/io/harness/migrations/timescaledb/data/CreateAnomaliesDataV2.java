/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.timescaledb.data;

import io.harness.migrations.timescaledb.AbstractTimeScaleDBMigration;

public class CreateAnomaliesDataV2 extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_anomalies_v2_data_table.sql";
  }
}
