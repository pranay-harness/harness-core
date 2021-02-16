package io.harness.cvng.core.services.api;

public interface DataSourceService {
  String checkConnectivity(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier, String tracingId);
}
