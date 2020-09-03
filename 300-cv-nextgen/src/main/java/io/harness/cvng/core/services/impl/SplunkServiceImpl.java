package io.harness.cvng.core.services.impl;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.RequestExecutor;
import io.harness.cvng.client.VerificationManagerClient;
import io.harness.cvng.core.services.api.SplunkService;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;

import java.util.List;
import java.util.Optional;

public class SplunkServiceImpl implements SplunkService {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private RequestExecutor requestExecutor;
  @Inject private NextGenService nextGenService;

  @Override
  public List<SplunkSavedSearch> getSavedSearches(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String requestGuid) {
    SplunkConnectorDTO splunkConnectorDTO =
        retrieveSplunkConnectorDTO(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    return requestExecutor
        .execute(verificationManagerClient.getSavedSearches(
            accountId, connectorIdentifier, orgIdentifier, projectIdentifier, requestGuid, splunkConnectorDTO))
        .getResource();
  }

  @Override
  public SplunkValidationResponse getValidationResponse(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, String query, String requestGuid) {
    SplunkConnectorDTO splunkConnectorDTO =
        retrieveSplunkConnectorDTO(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    return requestExecutor
        .execute(verificationManagerClient.getSamples(
            accountId, connectorIdentifier, orgIdentifier, projectIdentifier, query, requestGuid, splunkConnectorDTO))
        .getResource();
  }

  private SplunkConnectorDTO retrieveSplunkConnectorDTO(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<ConnectorDTO> connectorDTO =
        nextGenService.get(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    Preconditions.checkState(connectorDTO.isPresent(), "ConnectorDTO should not be null");
    Preconditions.checkState(connectorDTO.get().getConnectorConfig() instanceof SplunkConnectorDTO,
        "ConnectorConfig should be of type Splunk");
    return (SplunkConnectorDTO) connectorDTO.get().getConnectorConfig();
  }
}
