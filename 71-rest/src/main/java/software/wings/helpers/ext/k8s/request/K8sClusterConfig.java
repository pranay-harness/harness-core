package software.wings.helpers.ext.k8s.request;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.GcpKubernetesCluster;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.settings.SettingValue;

import java.util.List;

@Data
@Builder
public class K8sClusterConfig implements ExecutionCapabilityDemander {
  private SettingValue cloudProvider;
  private List<EncryptedDataDetail> cloudProviderEncryptionDetails;
  private AzureKubernetesCluster azureKubernetesCluster;
  private GcpKubernetesCluster gcpKubernetesCluster;
  private String clusterName;
  private String namespace;
  private String cloudProviderName;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(cloudProvider, cloudProviderEncryptionDetails);
  }
}
