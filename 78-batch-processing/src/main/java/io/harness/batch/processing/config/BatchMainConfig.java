package io.harness.batch.processing.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.mongo.MongoConfig;
import io.harness.timescaledb.TimeScaleDBConfig;
import lombok.Builder;
import lombok.Data;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.security.authentication.AwsS3SyncConfig;
import software.wings.security.authentication.BatchQueryConfig;

@Data
@Builder
public class BatchMainConfig {
  @JsonProperty("timescaledb") private TimeScaleDBConfig timeScaleDBConfig;
  @JsonProperty("harness-mongo") private MongoConfig harnessMongo;
  @JsonProperty("events-mongo") private MongoConfig eventsMongo;
  @JsonProperty("batchQueryConfig") private BatchQueryConfig batchQueryConfig;
  @JsonProperty("awsS3SyncConfig") private AwsS3SyncConfig awsS3SyncConfig;
  @JsonProperty("billingDataPipelineConfig") private BillingDataPipelineConfig billingDataPipelineConfig;
  @JsonProperty("smtp") private SmtpConfig smtpConfig;
  @JsonProperty("baseUrl") private String baseUrl;
}
