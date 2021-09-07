package io.harness.cvng.metrics.beans;

import io.harness.metrics.AutoMetricContext;

public class DataCollectionTaskMetricContext extends AutoMetricContext {
  public DataCollectionTaskMetricContext(
      String accountId, String dataCollectionTaskType, String provider, long retryCount) {
    put("accountId", accountId);
    put("dataCollectionTaskType", dataCollectionTaskType);
    put("provider", provider);
    put("retryCount", String.valueOf(retryCount));
  }
}
