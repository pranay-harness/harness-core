package io.harness.delegate.task.nexus;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusValidationParams;
import io.harness.task.ConnectorValidationHandler;
import io.harness.nexus.NexusClientImpl;
import io.harness.nexus.NexusRequest;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class NexusValidationHandler implements ConnectorValidationHandler {
  @Inject private SecretDecryptionService decryptionService;
  @Inject NexusClientImpl nexusClient;
  @Inject NexusMapper nexusMapper;

  @Override
  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    final NexusValidationParams taskParams = (NexusValidationParams) connectorValidationParams;
    final NexusConnectorDTO nexusConfig = taskParams.getNexusConnectorDTO();
    final List<EncryptedDataDetail> encryptionDetails = taskParams.getEncryptedDataDetails();
    decryptionService.decrypt(nexusConfig.getAuth().getCredentials(), encryptionDetails);
    final NexusRequest nexusRequest = nexusMapper.toNexusRequest(nexusConfig);

    ConnectorValidationResult connectorValidationResult;
    boolean running = nexusClient.isRunning(nexusRequest);
    if (running) {
      connectorValidationResult = ConnectorValidationResult.builder()
                                      .status(ConnectivityStatus.SUCCESS)
                                      .testedAt(System.currentTimeMillis())
                                      .build();
    } else {
      connectorValidationResult = ConnectorValidationResult.builder()
                                      .status(ConnectivityStatus.FAILURE)
                                      .testedAt(System.currentTimeMillis())
                                      .build();
    }
    return connectorValidationResult;
  }
}
