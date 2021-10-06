package io.harness.connector.validator.scmValidators;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.validator.AbstractConnectorValidator;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

@OwnedBy(HarnessTeam.DX)
public abstract class AbstractKubernetesConnectorValidator extends AbstractConnectorValidator {
  protected List<EncryptedDataDetail> fetchEncryptionDetailsList(KubernetesClusterConfigDTO kubernetesClusterConfig,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (kubernetesClusterConfig.getCredential().getKubernetesCredentialType()
        == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesAuthCredentialDTO kubernetesAuthCredential = getKubernetesAuthCredential(
          (KubernetesClusterDetailsDTO) kubernetesClusterConfig.getCredential().getConfig());
      return super.getEncryptionDetail(kubernetesAuthCredential, accountIdentifier, orgIdentifier, projectIdentifier);
    }
    return null;
  }

  private KubernetesAuthCredentialDTO getKubernetesAuthCredential(
      KubernetesClusterDetailsDTO kubernetesClusterConfigDTO) {
    return kubernetesClusterConfigDTO.getAuth().getCredentials();
  }
}
