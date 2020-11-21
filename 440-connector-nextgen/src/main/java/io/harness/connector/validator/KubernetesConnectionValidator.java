package io.harness.connector.validator;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
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

import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class KubernetesConnectionValidator
    extends AbstractConnectorValidator implements ConnectionValidator<KubernetesClusterConfigDTO> {
  public ConnectorValidationResult validate(KubernetesClusterConfigDTO kubernetesClusterConfig,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    DelegateResponseData responseData =
        super.validateConnector(kubernetesClusterConfig, accountIdentifier, orgIdentifier, projectIdentifier);
    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      log.info("Error in validation task for connector : [{}] with failure types [{}]",
          errorNotifyResponseData.getErrorMessage(), errorNotifyResponseData.getFailureTypes());
    }
    KubernetesConnectionTaskResponse taskResponse = (KubernetesConnectionTaskResponse) responseData;
    return ConnectorValidationResult.builder()
        .valid(taskResponse.getConnectionSuccessFul())
        .errorMessage(taskResponse.getErrorMessage())
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
    if (kubernetesClusterConfig.getCredential().getKubernetesCredentialType()
        == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesAuthCredentialDTO kubernetesAuthCredential = getKubernetesAuthCredential(
          (KubernetesClusterDetailsDTO) kubernetesClusterConfig.getCredential().getConfig());
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
