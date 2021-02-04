package io.harness.delegate.beans.connector.k8Connector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesClusterConfigDTO extends ConnectorConfigDTO {
  @Valid @NotNull KubernetesCredentialDTO credential;

  @Override
  public DecryptableEntity getDecryptableEntity() {
    if (credential.getKubernetesCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesClusterDetailsDTO k8sManualCreds = (KubernetesClusterDetailsDTO) credential.getConfig();
      return k8sManualCreds.getAuth().getCredentials();
    }
    return null;
  }
}
