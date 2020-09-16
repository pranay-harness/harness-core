package io.harness.batch.processing.anomalydetection;

import io.harness.batch.processing.anomalydetection.types.EntityType;
import io.harness.batch.processing.anomalydetection.types.TimeGranularity;
import io.harness.batch.processing.pricing.data.CloudProvider;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class AnomalyDetectionInfo {
  private String accountId;
  private TimeGranularity timeGranularity;
  private String entityId;
  private EntityType entityType;
  private String clusterId;
  private String clusterName;
  private String workloadName;
  private String workloadType;
  private String namespace;
  private CloudProvider cloudProvider;
}
