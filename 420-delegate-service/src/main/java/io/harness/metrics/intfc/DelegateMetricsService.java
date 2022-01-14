package io.harness.metrics.intfc;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateTaskResponse;

public interface DelegateMetricsService {
  void recordDelegateTaskMetrics(DelegateTask task, String metricName);

  void recordDelegateTaskMetrics(String accountId, String delegateId, String metricName);

  void recordDelegateTaskResponseMetrics(DelegateTask delegateTask, DelegateTaskResponse response, String metricName);

  void recordDelegateMetrics(Delegate delegate, String metricName);
}
