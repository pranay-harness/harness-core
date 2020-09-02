package io.harness.batch.processing.billing.timeseries.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PodCountData {
  private String accountId;
  private String clusterId;
  private String nodeId;
  private long startTime;
  private long endTime;
  private long count;
}
