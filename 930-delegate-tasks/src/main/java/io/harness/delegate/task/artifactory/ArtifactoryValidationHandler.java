package io.harness.delegate.task.artifactory;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifactory.ArtifactoryClientImpl;
import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.task.ConnectorValidationHandler;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryValidationParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(CDC)
@Singleton
public class ArtifactoryValidationHandler implements ConnectorValidationHandler {
  @Inject ArtifactoryClientImpl artifactoryService;
  @Inject SecretDecryptionService decryptionService;
  @Inject ArtifactoryRequestMapper artifactoryRequestMapper;

  @Override
  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    final ArtifactoryValidationParams artifactoryValidationParams =
        (ArtifactoryValidationParams) connectorValidationParams;
    final ArtifactoryConnectorDTO artifactoryConnectorDTO =
        ((ArtifactoryValidationParams) connectorValidationParams).getArtifactoryConnectorDTO();
    final List<EncryptedDataDetail> encryptedDataDetails = artifactoryValidationParams.getEncryptedDataDetails();
    if (isNotEmpty(artifactoryConnectorDTO.getDecryptableEntities())) {
      decryptionService.decrypt(artifactoryConnectorDTO.getDecryptableEntities().get(0), encryptedDataDetails);
    }
    final ArtifactoryConfigRequest artifactoryConfigRequest =
        artifactoryRequestMapper.toArtifactoryRequest(artifactoryConnectorDTO);
    ConnectorValidationResult connectorValidationResult;
    boolean running = artifactoryService.validateArtifactServer(artifactoryConfigRequest);
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
