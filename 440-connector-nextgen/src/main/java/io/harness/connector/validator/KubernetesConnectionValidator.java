package io.harness.connector.validator;

import static software.wings.beans.TaskType.VALIDATE_KUBERNETES_CONFIG;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.validator.scmValidators.AbstractKubernetesConnectorValidator;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.DX)
public class KubernetesConnectionValidator extends AbstractKubernetesConnectorValidator {
  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO kubernetesClusterConfig, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    DelegateResponseData responseData = super.validateConnector(
        kubernetesClusterConfig, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    KubernetesConnectionTaskResponse taskResponse = (KubernetesConnectionTaskResponse) responseData;
    return taskResponse.getConnectorValidationResult();
  }

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    KubernetesClusterConfigDTO kubernetesClusterConfig = (KubernetesClusterConfigDTO) connectorConfig;
    List<EncryptedDataDetail> encryptedDataDetailList =
        super.fetchEncryptionDetailsList(kubernetesClusterConfig, accountIdentifier, orgIdentifier, projectIdentifier);

    return KubernetesConnectionTaskParams.builder()
        .kubernetesClusterConfig(kubernetesClusterConfig)
        .encryptionDetails(encryptedDataDetailList)
        .build();
  }

  @Override
  public String getTaskType() {
    return VALIDATE_KUBERNETES_CONFIG.name();
  }
}
