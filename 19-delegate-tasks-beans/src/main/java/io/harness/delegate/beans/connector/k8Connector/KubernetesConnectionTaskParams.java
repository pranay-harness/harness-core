package io.harness.delegate.beans.connector.k8Connector;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@Builder
public class KubernetesConnectionTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  KubernetesClusterConfigDTO kubernetesClusterConfig;
  private List<EncryptedDataDetail> encryptionDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    if (kubernetesClusterConfig.getCredential().getKubernetesCredentialType()
        == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      KubernetesDelegateDetailsDTO k8sDetails =
          (KubernetesDelegateDetailsDTO) kubernetesClusterConfig.getCredential().getConfig();
      return Collections.singletonList(SystemEnvCheckerCapability.builder()
                                           .comparate(k8sDetails.getDelegateName())
                                           .systemPropertyName("DELEGATE_NAME")
                                           .build());
    }
    KubernetesClusterDetailsDTO k8sManualCreds =
        (KubernetesClusterDetailsDTO) kubernetesClusterConfig.getCredential().getConfig();
    return Collections.singletonList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        k8sManualCreds.getMasterUrl()));
  }
}
