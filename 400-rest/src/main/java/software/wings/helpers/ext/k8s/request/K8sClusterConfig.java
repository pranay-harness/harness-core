package software.wings.helpers.ext.k8s.request;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.GcpKubernetesCluster;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.RancherConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.settings.SettingValue;

import java.util.ArrayList;
import java.util.List;

import static io.harness.annotations.dev.HarnessModule._950_DELEGATE_TASKS_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

@Data
@Builder
@OwnedBy(CDP)
@TargetModule(_950_DELEGATE_TASKS_BEANS)
public class K8sClusterConfig implements ExecutionCapabilityDemander {
  private SettingValue cloudProvider;
  private List<EncryptedDataDetail> cloudProviderEncryptionDetails;
  private AzureKubernetesCluster azureKubernetesCluster;
  private GcpKubernetesCluster gcpKubernetesCluster;
  private String clusterName;
  private String namespace;
  private String cloudProviderName;
  private String masterUrl;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    if (cloudProvider instanceof KubernetesClusterConfig || cloudProvider instanceof RancherConfig) {
      return CapabilityHelper.generateDelegateCapabilities(
          cloudProvider, cloudProviderEncryptionDetails, maskingEvaluator);
    }
    List<ExecutionCapability> capabilities = new ArrayList<>();
    capabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        getMasterUrl(), maskingEvaluator));
    capabilities.addAll(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
        getCloudProviderEncryptionDetails(), maskingEvaluator));
    return capabilities;
  }
}