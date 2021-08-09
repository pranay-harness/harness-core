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
