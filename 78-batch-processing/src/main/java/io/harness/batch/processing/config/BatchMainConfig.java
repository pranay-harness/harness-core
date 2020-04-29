package io.harness.batch.processing.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.mongo.MongoConfig;
import io.harness.timescaledb.TimeScaleDBConfig;
import lombok.Builder;
import lombok.Data;
import software.wings.security.authentication.AwsS3SyncConfig;

@Data
@Builder
public class BatchMainConfig {
  @JsonProperty("timescaledb") private TimeScaleDBConfig timeScaleDBConfig;
  @JsonProperty("harness-mongo") private MongoConfig harnessMongo;
  @JsonProperty("events-mongo") private MongoConfig eventsMongo;
  @JsonProperty("awsS3SyncConfig") private AwsS3SyncConfig awsS3SyncConfig;
  @JsonProperty("awsDataPipelineConfig") private AwsDataPipelineConfig awsDataPipelineConfig;
}
