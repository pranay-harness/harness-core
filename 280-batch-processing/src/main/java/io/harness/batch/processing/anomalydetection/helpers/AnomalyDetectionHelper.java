package io.harness.batch.processing.anomalydetection.helpers;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.service.impl.AnomalyDetectionLogContext;
import io.harness.ccm.anomaly.entities.EntityType;
import io.harness.logging.AutoLogContext;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnomalyDetectionHelper {
  private AnomalyDetectionHelper() {}

  public static String generateHash(String originalString) {
    return Hashing.sha256().hashString(originalString, StandardCharsets.UTF_8).toString();
  }

  public static void logInvalidTimeSeries(AnomalyDetectionTimeSeries timeSeries) {
    if (timeSeries == null) {
      return;
    }
    try (AutoLogContext ignore = new AnomalyDetectionLogContext(timeSeries.getId(), OVERRIDE_ERROR)) {
      EntityType type = timeSeries.getEntityType();
      switch (type) {
        case CLUSTER:
          log.warn("Invalid Data for TimeSeries :: AccountId : {} , clusterName : {} , clusterId : {} ",
              timeSeries.getAccountId(), timeSeries.getClusterName(), timeSeries.getClusterId());
          break;
        case NAMESPACE:
          log.warn(
              "Invalid Data for TimeSeries :: AccountId : {} , clusterName : {} , clusterId : {} , namespace : {} ",
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
  }

  public static void logValidTimeSeries(AnomalyDetectionTimeSeries timeSeries) {
    if (timeSeries == null) {
      return;
    }

    try (AutoLogContext ignore = new AnomalyDetectionLogContext(timeSeries.getId(), OVERRIDE_ERROR)) {
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

  public static void logProcessingTimeSeries(String model) {
    log.info("Processing time series using {}", model);
  }

  public static void logUnsuccessfulHttpCall(Integer code, String error) {
    log.info("unsuccessful http request from python server , error code {}", code);
    log.error(error);
  }
}
