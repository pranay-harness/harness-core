package io.harness.cvng.core.services.api;

public interface DataSourceConnectivityChecker {
  void checkConnectivity(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String tracingId);
}
