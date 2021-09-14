/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
