package io.harness.batch.processing.billing.timeseries.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsageTimeInfo {
  private long time;
  private String type;
}
