package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.settings.SettingValue;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class ClusterMasterUrlValidationCapability implements ExecutionCapability {
  @NotNull ContainerServiceParams containerServiceParams;
  CapabilityType capabilityType = CapabilityType.CLUSTER_MASTER_URL;

  @Override
  public String fetchCapabilityBasis() {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (value instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) value;
      return kubernetesClusterConfig.getMasterUrl();
    } else if (value instanceof KubernetesConfig) {
      return ((KubernetesConfig) value).getMasterUrl();
    } else if (value instanceof GcpConfig) {
      return "GCP:" + containerServiceParams.getClusterName();
    } else if (value instanceof AzureConfig) {
      String subscriptionId = containerServiceParams.getSubscriptionId();
      String resourceGroup = containerServiceParams.getResourceGroup();
      return "Azure:" + subscriptionId + resourceGroup + containerServiceParams.getClusterName();
    } else {
      throw new InvalidRequestException("No capability Basis Supported for : " + value.getSettingType());
    }
  }
}
