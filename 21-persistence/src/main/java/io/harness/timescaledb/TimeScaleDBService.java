package io.harness.timescaledb;

import java.sql.Connection;

public interface TimeScaleDBService {
  Connection getDBConnection();

  /**
   * Temporary method to check if db is available. Will be deprecated once this is available everywhere and TimeScaleDB
   * is mandatory
   * @return
   */
  boolean isValid();
}
