package io.harness.cvng.client;

import com.google.inject.Inject;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.InternalServerErrorException;

public class VerificationManagerServiceImpl implements VerificationManagerService {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private RequestExecutor requestExecutor;
  @Inject private NextGenService nextGenService;
  @Override
  public String createServiceGuardPerpetualTask(String accountId, String cvConfigId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, String dataCollectionWorkerId) {
    // Need to write this to handle retries, exception etc in a proper way.

    Optional<ConnectorDTO> connectorDTO =
        nextGenService.get(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    if (!connectorDTO.isPresent()) {
      throw new InternalServerErrorException("Failed to retrieve connector with id: " + connectorIdentifier);
    }

    Map<String, String> params = new HashMap<>();
    params.put(DataCollectionTaskKeys.dataCollectionWorkerId, dataCollectionWorkerId);
    params.put(DataCollectionTaskKeys.cvConfigId, cvConfigId);
    params.put(CVConfigKeys.connectorIdentifier, connectorIdentifier);

    DataCollectionConnectorBundle bundle =
        DataCollectionConnectorBundle.builder().connectorDTO(connectorDTO.get()).params(params).build();

    return requestExecutor
        .execute(verificationManagerClient.createDataCollectionPerpetualTask(
            accountId, orgIdentifier, projectIdentifier, bundle))
        .getResource();
  }

  @Override
  public String createDeploymentVerificationPerpetualTask(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, String dataCollectionWorkerId) {
    // TODO(telemetry): counter
    Optional<ConnectorDTO> connectorDTO =
        nextGenService.get(accountId, connectorIdentifier, orgIdentifier, projectIdentifier);
    if (!connectorDTO.isPresent()) {
      throw new InternalServerErrorException("Failed to retrieve connector with id: " + connectorIdentifier);
    }

    Map<String, String> params = new HashMap<>();
    params.put(DataCollectionTaskKeys.dataCollectionWorkerId, dataCollectionWorkerId);
    params.put(CVConfigKeys.connectorIdentifier, connectorIdentifier);

    DataCollectionConnectorBundle bundle =
        DataCollectionConnectorBundle.builder().connectorDTO(connectorDTO.get()).params(params).build();

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
}
