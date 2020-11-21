package io.harness.cvng.client;

import static io.harness.cvng.beans.DataCollectionType.KUBERNETES;

import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.InternalServerErrorException;

public class VerificationManagerServiceImpl implements VerificationManagerService {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private RequestExecutor requestExecutor;
  @Inject private NextGenService nextGenService;
  @Override
  public String createDataCollectionTask(String accountId, String orgIdentifier, String projectIdentifier,
      DataCollectionType dataCollectionType, Map<String, String> params) {
    // Need to write this to handle retries, exception etc in a proper way.
    Preconditions.checkState(params.containsKey(CVConfigKeys.connectorIdentifier));
    Optional<ConnectorInfoDTO> connectorDTO =
        nextGenService.get(accountId, params.get(CVConfigKeys.connectorIdentifier), orgIdentifier, projectIdentifier);
    if (!connectorDTO.isPresent()) {
      throw new InternalServerErrorException(
          "Failed to retrieve connector with id: " + params.get(CVConfigKeys.connectorIdentifier));
    }

    DataCollectionConnectorBundle bundle = DataCollectionConnectorBundle.builder()
                                               .connectorDTO(connectorDTO.get())
                                               .params(params)
                                               .dataCollectionType(dataCollectionType)
                                               .build();

    return requestExecutor
        .execute(verificationManagerClient.createDataCollectionPerpetualTask(
            accountId, orgIdentifier, projectIdentifier, bundle))
        .getResource();
  }

  @Override
  public void deletePerpetualTask(String accountId, String perpetualTaskId) {
    requestExecutor.execute(verificationManagerClient.deleteDataCollectionPerpetualTask(accountId, perpetualTaskId));
  }

  @Override
  public void deletePerpetualTasks(String accountId, List<String> perpetualTaskIds) {
    perpetualTaskIds.forEach(dataCollectionWorkerId -> this.deletePerpetualTask(accountId, dataCollectionWorkerId));
  }

  @Override
  public String getDataCollectionResponse(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionRequest request) {
    return requestExecutor
        .execute(
            verificationManagerClient.getDataCollectionResponse(accountId, orgIdentifier, projectIdentifier, request))
        .getResource();
  }

  @Override
  public List<String> getKubernetesNamespaces(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String filter) {
    Optional<ConnectorInfoDTO> connectorDTO =
        nextGenService.get(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    if (!connectorDTO.isPresent()) {
      throw new InternalServerErrorException("Failed to retrieve connector with id: " + connectorIdentifier);
    }

    DataCollectionConnectorBundle bundle = DataCollectionConnectorBundle.builder()
                                               .connectorDTO(connectorDTO.get())
                                               .params(Collections.emptyMap())
                                               .dataCollectionType(KUBERNETES)
                                               .build();

    return requestExecutor
        .execute(verificationManagerClient.getKubernetesNamespaces(
            accountId, orgIdentifier, projectIdentifier, filter, bundle))
        .getResource();
  }

  @Override
  public List<String> getKubernetesWorkloads(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String namespace, String filter) {
    Optional<ConnectorInfoDTO> connectorDTO =
        nextGenService.get(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    if (!connectorDTO.isPresent()) {
      throw new InternalServerErrorException("Failed to retrieve connector with id: " + connectorIdentifier);
    }

    DataCollectionConnectorBundle bundle = DataCollectionConnectorBundle.builder()
                                               .connectorDTO(connectorDTO.get())
                                               .params(Collections.emptyMap())
                                               .dataCollectionType(KUBERNETES)
                                               .build();

    return requestExecutor
        .execute(verificationManagerClient.getKubernetesWorkloads(
            accountId, orgIdentifier, projectIdentifier, namespace, filter, bundle))
        .getResource();
  }
}
