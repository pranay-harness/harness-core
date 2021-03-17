package io.harness.delegate.beans.connector.k8Connector;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesClusterConfigDTO extends ConnectorConfigDTO implements DelegateSelectable {
  @Valid @NotNull KubernetesCredentialDTO credential;
  Set<String> delegateSelectors;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    if (credential.getKubernetesCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesClusterDetailsDTO k8sManualCreds = (KubernetesClusterDetailsDTO) credential.getConfig();
      return Collections.singletonList(k8sManualCreds.getAuth().getCredentials());
    }
    return null;
  }
}
