package io.harness.cvng.core.services.api;

import java.util.List;

public interface PrometheusService {
  List<String> getMetricNames(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier, String tracingId);
  List<String> getLabelNames(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier, String tracingId);
  List<String> getLabelValues(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String labelName, String tracingId);
}
