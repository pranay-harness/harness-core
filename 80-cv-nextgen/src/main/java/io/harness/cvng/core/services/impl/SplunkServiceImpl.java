package io.harness.cvng.core.services.impl;

import com.google.inject.Inject;

import io.harness.cvng.beans.CVHistogram;
import io.harness.cvng.beans.SplunkSampleResponse;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.client.RequestExecutor;
import io.harness.cvng.client.VerificationManagerClient;
import io.harness.cvng.core.services.api.SplunkService;

import java.util.List;

public class SplunkServiceImpl implements SplunkService {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private RequestExecutor requestExecutor;
  @Override
  public List<SplunkSavedSearch> getSavedSearches(String accountId, String connectorId, String requestGuid) {
    return requestExecutor.execute(verificationManagerClient.getSavedSearches(accountId, connectorId, requestGuid))
        .getResource();
  }

  @Override
  public CVHistogram getHistogram(String accountId, String connectorId, String query, String requestGuid) {
    return requestExecutor.execute(verificationManagerClient.getHistogram(accountId, connectorId, query, requestGuid))
        .getResource();
  }

  @Override
  public SplunkSampleResponse getSamples(String accountId, String connectorId, String query, String requestGuid) {
    return requestExecutor.execute(verificationManagerClient.getSamples(accountId, connectorId, query, requestGuid))
        .getResource();
  }
}
