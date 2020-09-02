package io.harness.batch.processing.billing.timeseries.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PodActivityInfo {
  private String podId;
  private long usageStartTime;
  private long usageStopTime;
}
