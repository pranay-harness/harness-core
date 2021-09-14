/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.timescaledb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@FieldNameConstants(innerTypeName = "TimeScaleDBConfigFields")
public class TimeScaleDBConfig {
  @JsonProperty(defaultValue = "jdbc:postgresql://localhost:5432/harness") @NotEmpty private String timescaledbUrl;
  private String timescaledbUsername;
  private String timescaledbPassword;
  int connectTimeout;
  int socketTimeout;
  boolean logUnclosedConnections;
  private String loggerLevel;
  private int instanceDataRetentionDays;
  private int instanceStatsMigrationEventsLimit;
  private int instanceStatsMigrationQueryBatchSize;
  private int deploymentDataMigrationRowLimit;
  private int deploymentDataMigrationQueryBatchSize;
  boolean isHealthCheckNeeded;
}
