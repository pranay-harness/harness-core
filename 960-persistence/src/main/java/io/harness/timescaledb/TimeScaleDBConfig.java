package io.harness.timescaledb;

import io.harness.secret.ConfigSecret;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@FieldNameConstants(innerTypeName = "TimeScaleDBConfigFields")
@FieldDefaults(makeFinal = false)
public class TimeScaleDBConfig {
  @JsonProperty(defaultValue = "jdbc:postgresql://localhost:5432/harness")
  @NotEmpty
  @ConfigSecret
  private String timescaledbUrl;
  @ConfigSecret private String timescaledbUsername;
  @ConfigSecret private String timescaledbPassword;
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
