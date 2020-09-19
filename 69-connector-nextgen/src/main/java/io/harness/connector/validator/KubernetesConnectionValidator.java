package io.harness.connector.validator;

import com.google.inject.Singleton;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Singleton
public class KubernetesConnectionValidator
    extends AbstractConnectorValidator implements ConnectionValidator<KubernetesClusterConfigDTO> {
  public ConnectorValidationResult validate(KubernetesClusterConfigDTO kubernetesClusterConfig,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    KubernetesConnectionTaskResponse responseData = (KubernetesConnectionTaskResponse) super.validateConnector(
        kubernetesClusterConfig, accountIdentifier, orgIdentifier, projectIdentifier);
    return ConnectorValidationResult.builder()
        .valid(responseData.getConnectionSuccessFul())
        .errorMessage(responseData.getErrorMessage())
        .build();
  }

  private KubernetesAuthCredentialDTO getKubernetesAuthCredential(
      KubernetesClusterDetailsDTO kubernetesClusterConfigDTO) {
    return kubernetesClusterConfigDTO.getAuth().getCredentials();
  }

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<EncryptedDataDetail> encryptedDataDetailList = null;
    KubernetesClusterConfigDTO kubernetesClusterConfig = (KubernetesClusterConfigDTO) connectorConfig;
    if (kubernetesClusterConfig.getKubernetesCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesAuthCredentialDTO kubernetesAuthCredential =
          getKubernetesAuthCredential((KubernetesClusterDetailsDTO) kubernetesClusterConfig.getConfig());
      encryptedDataDetailList =
          super.getEncryptionDetail(kubernetesAuthCredential, accountIdentifier, orgIdentifier, projectIdentifier);
    }
    return KubernetesConnectionTaskParams.builder()
        .kubernetesClusterConfig(kubernetesClusterConfig)
        .encryptionDetails(encryptedDataDetailList)
        .build();
  }

  @Override
  public String getTaskType() {
    return "VALIDATE_KUBERNETES_CONFIG";
  }
}
