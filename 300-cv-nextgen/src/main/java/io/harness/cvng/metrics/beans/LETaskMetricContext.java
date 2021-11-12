package io.harness.cvng.metrics.beans;

import io.harness.metrics.AutoMetricContext;

public class LETaskMetricContext extends AutoMetricContext {
  public LETaskMetricContext(String accountId, String leTaskType) {
    put("accountId", accountId);
    put("leTaskType", leTaskType);
  }
}
