package io.harness.connector.heartbeat;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.helper.EncryptionHelper;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusValidationParams;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class NexusValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Inject EncryptionHelper encryptionHelper;

  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorInfoDTO, String connectorName,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final NexusConnectorDTO connectorConfig = (NexusConnectorDTO) connectorInfoDTO.getConnectorConfig();
    final List<EncryptedDataDetail> encryptionDetail = encryptionHelper.getEncryptionDetail(
        connectorConfig.getDecryptableEntity(), accountIdentifier, orgIdentifier, projectIdentifier);
    return NexusValidationParams.builder()
        .connectorName(connectorName)
        .encryptedDataDetails(encryptionDetail)
        .nexusConnectorDTO(connectorConfig)
        .build();
  }
}
