package io.harness.batch.processing.anomalydetection;

import io.harness.batch.processing.anomalydetection.types.EntityType;
import io.harness.batch.processing.anomalydetection.types.TimeGranularity;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Data
@SuperBuilder
@Slf4j
public abstract class AnomalyDetectionInfo {
  private String accountId;
  private TimeGranularity timeGranularity;
  private EntityType entityType;
  private String clusterId;
  private String clusterName;
  private String workloadName;
  private String workloadType;
  private String namespace;

  private String region;
  private String cloudProvider;

  private String gcpProject;
  private String gcpSKUId;
  private String gcpSKUDescription;
  private String gcpProduct;

  private String awsAccount;
  private String awsService;
  private String awsInstanceType;
  private String awsUsageType;

  public String getEntityId() {
    switch (entityType) {
      case CLUSTER:
        return clusterId;
      case NAMESPACE:
        return namespace;
      case WORKLOAD:
        return workloadName;
      case GCP_PRODUCT:
        return gcpProduct;
      case GCP_SKU_ID:
        return gcpSKUId;
      case GCP_PROJECT:
        return gcpProject;
      case AWS_ACCOUNT:
        return awsAccount;
      case AWS_SERVICE:
        return awsService;
      default:
        log.error("Unknown Entity Type [{}] please add support for it ", entityType);
    }
    return null;
  }
}
