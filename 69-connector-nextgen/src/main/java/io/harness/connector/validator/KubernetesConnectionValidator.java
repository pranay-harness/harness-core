package io.harness.connector.validator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskParams;
import io.harness.delegate.beans.connector.k8Connector.KubernetesConnectionTaskResponse;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class KubernetesConnectionValidator implements ConnectionValidator<KubernetesClusterConfigDTO> {
  private final DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  private final SecretManagerClientService ngSecretService;

  public ConnectorValidationResult validate(KubernetesClusterConfigDTO kubernetesClusterConfig,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    KubernetesAuthCredentialDTO kubernetesAuthCredential =
        getKubernetesAuthCredential((KubernetesClusterDetailsDTO) kubernetesClusterConfig.getConfig());
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();
    List<EncryptedDataDetail> encryptedDataDetailList =
        ngSecretService.getEncryptionDetails(basicNGAccessObject, kubernetesAuthCredential);
    DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                  .accountId(accountIdentifier)
                                                  .taskType("VALIDATE_KUBERNETES_CONFIG")
                                                  .taskParameters(KubernetesConnectionTaskParams.builder()
                                                                      .kubernetesClusterConfig(kubernetesClusterConfig)
                                                                      .encryptionDetails(encryptedDataDetailList)
                                                                      .build())
                                                  .executionTimeout(Duration.ofMinutes(1))
                                                  .build();
    KubernetesConnectionTaskResponse responseData =
        (KubernetesConnectionTaskResponse) delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
    return ConnectorValidationResult.builder()
        .valid(responseData.getConnectionSuccessFul())
        .errorMessage(responseData.getErrorMessage())
        .build();
  }

  private KubernetesAuthCredentialDTO getKubernetesAuthCredential(
      KubernetesClusterDetailsDTO kubernetesClusterConfigDTO) {
    return kubernetesClusterConfigDTO.getAuth().getCredentials();
  }
}
