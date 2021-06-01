package io.harness.batch.processing.config;

import io.harness.cf.CfClientConfig;
import io.harness.cf.CfMigrationConfig;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.mongo.MongoConfig;
import io.harness.remote.client.ServiceHttpClientConfig;
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
  @JsonProperty("azureStorageSyncConfig") private AzureStorageSyncConfig azureStorageSyncConfig;
  @JsonProperty("podInfo") private PodInfoConfig podInfoConfig;
  @JsonProperty("billingDataPipelineConfig") private BillingDataPipelineConfig billingDataPipelineConfig;
  @JsonProperty("smtp") private SmtpConfig smtpConfig;
  @JsonProperty("segmentConfig") private SegmentConfig segmentConfig;
  @JsonProperty("reportScheduleConfig") private ReportScheduleConfig reportScheduleConfig;
  @JsonProperty("baseUrl") private String baseUrl;
  @JsonProperty("scheduler-jobs-config") private SchedulerJobsConfig schedulerJobsConfig;
  @JsonProperty("cfClientConfig") private CfClientConfig cfClientConfig;
  @JsonProperty("cfMigrationConfig") private CfMigrationConfig cfMigrationConfig;
  @JsonProperty("cePythonService") private CEPythonServiceConfig cePythonServiceConfig;
  @JsonProperty("banzaiConfig") private BanzaiConfig banzaiConfig;
  @JsonProperty("ngManagerServiceHttpClientConfig") private ServiceHttpClientConfig ngManagerServiceHttpClientConfig;
  @JsonProperty("ngManagerServiceSecret") private String ngManagerServiceSecret;
  @JsonProperty("banzaiRecommenderConfig") private ServiceHttpClientConfig banzaiRecommenderConfig;
}
