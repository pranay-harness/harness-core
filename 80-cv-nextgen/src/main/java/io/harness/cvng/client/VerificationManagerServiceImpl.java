package io.harness.cvng.client;

import com.google.inject.Inject;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.InternalServerErrorException;

public class VerificationManagerServiceImpl implements VerificationManagerService {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private RequestExecutor requestExecutor;
  @Inject private NextGenService nextGenService;
  @Override
  public String createServiceGuardDataCollectionTask(String accountId, String cvConfigId, String connectorId,
      String orgIdentifier, String projectIdentifier, String dataCollectionWorkerId) {
    // Need to write this to handle retries, exception etc in a proper way.

    Optional<ConnectorDTO> connectorDTO = nextGenService.get(accountId, connectorId, orgIdentifier, projectIdentifier);
    if (!connectorDTO.isPresent()) {
      throw new InternalServerErrorException("Failed to retrieve connector with id: " + connectorId);
    }

    Map<String, String> params = new HashMap<>();
    params.put(DataCollectionTaskKeys.dataCollectionWorkerId, dataCollectionWorkerId);
    params.put(DataCollectionTaskKeys.cvConfigId, cvConfigId);
    params.put(CVConfigKeys.connectorId, connectorId);

    DataCollectionConnectorBundle bundle = DataCollectionConnectorBundle.builder()
                                               .connectorConfigDTO(connectorDTO.get().getConnectorConfig())
                                               .params(params)
                                               .build();

    return requestExecutor.execute(verificationManagerClient.create(accountId, bundle)).getResource();
  }

  @Override
  public String createDeploymentVerificationDataCollectionTask(String accountId, String verificationTaskId,
      String connectorId, String orgIdentifier, String projectIdentifier, String dataCollectionWorkerId) {
    Optional<ConnectorDTO> connectorDTO = nextGenService.get(accountId, connectorId, orgIdentifier, projectIdentifier);
    if (!connectorDTO.isPresent()) {
      throw new InternalServerErrorException("Failed to retrieve connector with id: " + connectorId);
    }

    Map<String, String> params = new HashMap<>();
    params.put(DataCollectionTaskKeys.dataCollectionWorkerId, dataCollectionWorkerId);
    params.put(CVConfigKeys.connectorId, connectorId);
    params.put(DataCollectionTaskKeys.verificationTaskId, verificationTaskId);

    DataCollectionConnectorBundle bundle = DataCollectionConnectorBundle.builder()
                                               .connectorConfigDTO(connectorDTO.get().getConnectorConfig())
                                               .params(params)
                                               .build();

    return requestExecutor.execute(verificationManagerClient.create(accountId, bundle)).getResource();
  }

  @Override
  public void deleteDataCollectionTask(String accountId, String taskId) {
    requestExecutor.execute(verificationManagerClient.deleteDataCollectionTask(accountId, taskId));
  }
}
