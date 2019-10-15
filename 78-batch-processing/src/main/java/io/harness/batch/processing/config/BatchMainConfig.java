package io.harness.batch.processing.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.mongo.MongoConfig;
import io.harness.timescaledb.TimeScaleDBConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchMainConfig {
  @JsonProperty("timescaledb") private TimeScaleDBConfig timeScaleDBConfig;
  @JsonProperty("mongo") private MongoConfig mongoConnectionFactory;
}
