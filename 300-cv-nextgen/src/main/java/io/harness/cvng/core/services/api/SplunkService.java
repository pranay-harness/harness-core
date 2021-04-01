package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;

import java.util.List;

public interface SplunkService extends MonitoringSourceImportStatusCreator, DataSourceConnectivityChecker {
  List<SplunkSavedSearch> getSavedSearches(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier, String requestGuid);

  SplunkValidationResponse getValidationResponse(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String query, String requestGuid);
}
