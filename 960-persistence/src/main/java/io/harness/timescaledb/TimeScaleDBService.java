/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.timescaledb;

import io.harness.health.HealthMonitor;

import java.sql.Connection;
import java.sql.SQLException;

public interface TimeScaleDBService extends HealthMonitor {
  Connection getDBConnection() throws SQLException;

  TimeScaleDBConfig getTimeScaleDBConfig();

  /**
   * Temporary method to check if db is available. Will be deprecated once this is available everywhere and TimeScaleDB
   * is mandatory
   * @return
   */
  boolean isValid();
}
