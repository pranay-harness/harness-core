package io.harness.batch.processing.svcmetrics;

import io.harness.metrics.AutoMetricContext;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class JobMetricContext extends AutoMetricContext {
  public JobMetricContext(String accountId, String batchJobType) {
    put("accountId", accountId);
    put("batchJobType", batchJobType);
  }
}
