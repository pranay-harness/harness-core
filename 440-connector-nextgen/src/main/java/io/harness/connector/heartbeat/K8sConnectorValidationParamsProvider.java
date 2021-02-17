package io.harness.connector.heartbeat;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.helper.EncryptionHelper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.k8Connector.K8sValidationParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class K8sConnectorValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Inject EncryptionHelper encryptionHelper;

  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorInfoDTO, String connectorName,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    ConnectorConfigDTO connectorConfigDTO = connectorInfoDTO.getConnectorConfig();
    final List<EncryptedDataDetail> encryptionDetail = encryptionHelper.getEncryptionDetail(
        connectorConfigDTO.getDecryptableEntity(), accountIdentifier, orgIdentifier, projectIdentifier);
    return K8sValidationParams.builder()
        .kubernetesClusterConfigDTO((KubernetesClusterConfigDTO) connectorConfigDTO)
        .connectorName(connectorName)
        .encryptedDataDetails(encryptionDetail)
        .build();
  }
}
