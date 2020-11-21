package io.harness.batch.processing.config;

import io.harness.event.handler.segment.SegmentConfig;
import io.harness.mongo.MongoConfig;
import io.harness.timescaledb.TimeScaleDBConfig;

import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.security.authentication.AwsS3SyncConfig;
import software.wings.security.authentication.BatchQueryConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchMainConfig {
  @JsonProperty("timescaledb") private TimeScaleDBConfig timeScaleDBConfig;
  @JsonProperty("harness-mongo") private MongoConfig harnessMongo;
  @JsonProperty("events-mongo") private MongoConfig eventsMongo;
  @JsonProperty("batchQueryConfig") private BatchQueryConfig batchQueryConfig;
  @JsonProperty("awsRegionIdToName") private Map<String, String> awsRegionIdToName;
  @JsonProperty("awsS3SyncConfig") private AwsS3SyncConfig awsS3SyncConfig;
  @JsonProperty("podInfo") private PodInfoConfig podInfoConfig;
  @JsonProperty("billingDataPipelineConfig") private BillingDataPipelineConfig billingDataPipelineConfig;
  @JsonProperty("smtp") private SmtpConfig smtpConfig;
  @JsonProperty("segmentConfig") private SegmentConfig segmentConfig;
  @JsonProperty("reportScheduleConfig") private ReportScheduleConfig reportScheduleConfig;
  @JsonProperty("baseUrl") private String baseUrl;
}
