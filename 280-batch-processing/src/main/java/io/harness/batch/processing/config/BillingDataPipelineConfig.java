package io.harness.batch.processing.config;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class BillingDataPipelineConfig {
  private String gcpProjectId;
  private String gcsBasePath;
  private String gcpPipelinePubSubTopic;
  private String gcpSyncPubSubTopic;
  private boolean gcpUseNewPipeline;
  private boolean isGcpSyncEnabled;
  private String clusterDataGcsBucketName;
  private String clusterDataGcsBackupBucketName;
  private boolean awsUseNewPipeline;
  private String awsRoleName;

  public String getGcpPipelinePubSubTopic() {
    return "projects/" + gcpProjectId + "/topics/" + gcpPipelinePubSubTopic;
  }
}
