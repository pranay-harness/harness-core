package io.harness.delegate.beans.connector.k8Connector;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.UnknownEnumTypeException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesClusterConfigDTO extends ConnectorConfigDTO implements ExecutionCapabilityDemander {
  KubernetesCredentialDTO credential;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    if (credential.getKubernetesCredentialType() == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      KubernetesDelegateDetailsDTO k8sDetails = (KubernetesDelegateDetailsDTO) credential.getConfig();
      return Collections.singletonList(SystemEnvCheckerCapability.builder()
                                           .comparate(k8sDetails.getDelegateName())
                                           .systemPropertyName("DELEGATE_NAME")
                                           .build());
    } else if (credential.getKubernetesCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesClusterDetailsDTO k8sManualCreds = (KubernetesClusterDetailsDTO) credential.getConfig();
      return Collections.singletonList(
          HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
              k8sManualCreds.getMasterUrl()));
    } else {
      throw new UnknownEnumTypeException(
          "Kubernetes Credential Type", String.valueOf(credential.getKubernetesCredentialType()));
    }
  }
}
