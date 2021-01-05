package io.harness.batch.processing.anomalydetection.helpers;

import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.types.EntityType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnomalyDetectionHelper {
  private AnomalyDetectionHelper() {}

  public static void logInvalidTimeSeries(AnomalyDetectionTimeSeries timeSeries) {
    if (timeSeries == null) {
      return;
    }
    EntityType type = timeSeries.getEntityType();
    switch (type) {
      case CLUSTER:
        log.warn("Invalid Data for TimeSeries :: AccountId : {} , clusterName : {} , clusterId : {} ",
            timeSeries.getAccountId(), timeSeries.getClusterName(), timeSeries.getClusterId());
        break;
      case NAMESPACE:
        log.warn("Invalid Data for TimeSeries :: AccountId : {} , clusterName : {} , clusterId : {} , namespace : {} ",
            timeSeries.getAccountId(), timeSeries.getClusterName(), timeSeries.getClusterId(),
            timeSeries.getNamespace());
        break;
      case GCP_PROJECT:
        log.warn("Invalid Data for TimeSeries :: AccountId : {} , gcpProject : {}  ", timeSeries.getAccountId(),
            timeSeries.getGcpProject());
        break;
      case GCP_PRODUCT:
        log.warn("Invalid Data for TimeSeries :: AccountId : {} , gcpProject : {} , gcpProduct : {} ",
            timeSeries.getAccountId(), timeSeries.getGcpProject(), timeSeries.getGcpProduct());
        break;
      case GCP_SKU_ID:
        log.warn(
            "Invalid Data for TimeSeries :: AccountId : {} , gcpProject : {} , gcpProduct : {} , gcpSkuDescription : {} ",
            timeSeries.getAccountId(), timeSeries.getGcpProject(), timeSeries.getGcpProduct(),
            timeSeries.getGcpSKUDescription());
        break;
      case AWS_ACCOUNT:
        log.warn("Invalid Data for TimeSeries :: AccountId : {} , awsAccount : {}", timeSeries.getAccountId(),
            timeSeries.getAwsAccount());
        break;
      case AWS_SERVICE:
        log.warn("Invalid Data for TimeSeries :: AccountId : {} , awsAccount : {} , awsService : {} ",
            timeSeries.getAccountId(), timeSeries.getAwsAccount(), timeSeries.getAwsService());
        break;
      case GCP_REGION:
      case AWS_USAGE_TYPE:
      case AWS_INSTANCE_TYPE:
      default:
        break;
    }
  }

  public static void logValidTimeSeries(AnomalyDetectionTimeSeries timeSeries) {
    if (timeSeries == null) {
      return;
    }
    EntityType type = timeSeries.getEntityType();
    switch (type) {
      case CLUSTER:
        log.info("Valid Data for TimeSeries :: AccountId : {} , clusterName : {} , clusterId : {} ",
            timeSeries.getAccountId(), timeSeries.getClusterName(), timeSeries.getClusterId());
        break;
      case NAMESPACE:
        log.info("Valid Data for TimeSeries :: AccountId : {} , clusterName : {} , clusterId : {} , namespace : {} ",
            timeSeries.getAccountId(), timeSeries.getClusterName(), timeSeries.getClusterId(),
            timeSeries.getNamespace());
        break;
      case GCP_PROJECT:
        log.info("Valid Data for TimeSeries :: AccountId : {} , gcpProject : {}  ", timeSeries.getAccountId(),
            timeSeries.getGcpProject());
        break;
      case GCP_PRODUCT:
        log.info("Valid Data for TimeSeries :: AccountId : {} , gcpProject : {} , gcpProduct : {} ",
            timeSeries.getAccountId(), timeSeries.getGcpProject(), timeSeries.getGcpProduct());
        break;
      case GCP_SKU_ID:
        log.info(
            "Valid Data for TimeSeries :: AccountId : {} , gcpProject : {} , gcpProduct : {} , gcpSkuDescription : {} ",
            timeSeries.getAccountId(), timeSeries.getGcpProject(), timeSeries.getGcpProduct(),
            timeSeries.getGcpSKUDescription());
        break;
      case AWS_ACCOUNT:
        log.info("Valid Data for TimeSeries :: AccountId : {} , awsAccount : {}", timeSeries.getAccountId(),
            timeSeries.getAwsAccount());
        break;
      case AWS_SERVICE:
        log.info("Valid Data for TimeSeries :: AccountId : {} , awsAccount : {} , awsService : {} ",
            timeSeries.getAccountId(), timeSeries.getAwsAccount(), timeSeries.getAwsService());
        break;
      case GCP_REGION:
      case AWS_USAGE_TYPE:
      case AWS_INSTANCE_TYPE:
      default:
        break;
    }
  }
}
