package io.harness.connector.validator;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ManagerDelegateServiceDriver;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.secretmanagerclient.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class KubernetesConnectionValidator implements ConnectionValidator<KubernetesClusterConfigDTO> {
  private final ManagerDelegateServiceDriver managerDelegateServiceDriver;
  private final SecretManagerClientService ngSecretService;

  public ConnectorValidationResult validate(KubernetesClusterConfigDTO kubernetesClusterConfig, String accountId) {
    if (kubernetesClusterConfig.getKubernetesCredentialType() == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      return ConnectorValidationResult.builder().valid(true).build();
    }

    Map<String, String> setupAbstractions = ImmutableMap.of("accountId", accountId);
    KubernetesAuthCredentialDTO kubernetesCredentialEncryptedSettings =
        getKubernetesCredentialsEncrypedSettings((KubernetesClusterDetailsDTO) kubernetesClusterConfig.getConfig());
    List<EncryptedDataDetail> encryptedDataDetailList =
        ngSecretService.getEncryptionDetails(kubernetesCredentialEncryptedSettings);
    TaskData taskData = TaskData.builder()
                            .async(false)
                            .taskType("VALIDATE_KUBERNETES_CONFIG")
                            .parameters(new Object[] {KubernetesConnectionTaskParams.builder()
                                                          .kubernetesClusterConfig(kubernetesClusterConfig)
                                                          .encryptionDetails(encryptedDataDetailList)
                                                          .build()})
                            .timeout(TimeUnit.MINUTES.toMillis(1))
                            .build();
    KubernetesConnectionTaskResponse responseData =
        managerDelegateServiceDriver.sendTask(accountId, setupAbstractions, taskData);
    return ConnectorValidationResult.builder()
        .valid(responseData.getConnectionSuccessFul())
        .errorMessage(responseData.getErrorMessage())
        .build();
  }

  private KubernetesAuthCredentialDTO getKubernetesCredentialsEncrypedSettings(
      KubernetesClusterDetailsDTO kubernetesClusterConfigDTO) {
    return kubernetesClusterConfigDTO.getAuth().getCredentials();
  }
}
