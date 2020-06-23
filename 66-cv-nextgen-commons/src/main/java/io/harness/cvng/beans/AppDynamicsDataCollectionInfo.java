package io.harness.cvng.beans;

import io.harness.cvng.core.services.entities.MetricPack;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppDynamicsDataCollectionInfo extends DataCollectionInfo {
  private long tierId;
  private long applicationId;
  private MetricPack metricPack;
}
