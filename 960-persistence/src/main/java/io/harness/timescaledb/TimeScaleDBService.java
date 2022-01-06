/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
