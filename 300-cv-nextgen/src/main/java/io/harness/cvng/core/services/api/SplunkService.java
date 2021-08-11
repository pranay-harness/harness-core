package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.SplunkSavedSearch;

import java.util.LinkedHashMap;
import java.util.List;

public interface SplunkService extends MonitoringSourceImportStatusCreator, DataSourceConnectivityChecker {
  List<SplunkSavedSearch> getSavedSearches(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String requestGuid);

  List<LinkedHashMap> getSampleData(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String query, String requestGuid);

  List<LinkedHashMap> getLatestHistogram(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String query, String requestGuid);
}
