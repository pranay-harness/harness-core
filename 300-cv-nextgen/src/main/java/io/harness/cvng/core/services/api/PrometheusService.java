package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.PrometheusSampleData;

import java.util.List;

public interface PrometheusService {
  List<String> getMetricNames(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier, String tracingId);
  List<String> getLabelNames(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier, String tracingId);
  List<String> getLabelValues(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String labelName, String tracingId);
  List<PrometheusSampleData> getSampleData(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String query, String tracingId);
}
