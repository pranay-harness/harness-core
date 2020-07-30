package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;

import java.util.List;

public interface SplunkService {
  List<SplunkSavedSearch> getSavedSearches(String accountId, String connectorId, String requestGuid);

  SplunkValidationResponse getValidationResponse(
      String accountId, String connectorId, String query, String requestGuid);
}
