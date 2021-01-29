package io.harness.connector.heartbeat;

import io.harness.connector.helper.EncryptionHelper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerValidationParams;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Inject;
import java.util.List;

public class DockerConnectorValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Inject EncryptionHelper encryptionHelper;
  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorConfigDTO connectorConfigDTO,
      String connectorName, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final List<EncryptedDataDetail> encryptionDetail = encryptionHelper.getEncryptionDetail(
        connectorConfigDTO.getDecryptableEntity(), accountIdentifier, orgIdentifier, projectIdentifier);
    return DockerValidationParams.builder()
        .dockerConnectorDTO((DockerConnectorDTO) connectorConfigDTO)
        .encryptionDataDetails(encryptionDetail)
        .connectorName(connectorName)
        .build();
  }
}
