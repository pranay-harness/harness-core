/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.timescaledb.data;

public class TimeScaleDBMigrationException extends RuntimeException {
  public TimeScaleDBMigrationException(Throwable cause) {
    super(cause);
  }
}
