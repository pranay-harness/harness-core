package software.wings.infra;

import static software.wings.beans.InfrastructureType.AZURE_KUBERNETES;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(AZURE_KUBERNETES)
public final class AzureKubernetesServiceYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private String clusterName;
  private String namespace;
  private String releaseName;
  private String resourceGroup;
  private String subscriptionId;

  @Builder
  public AzureKubernetesServiceYaml(String type, String cloudProviderName, String clusterName, String namespace,
      String releaseName, String resourceGroup, String subscriptionId) {
    super(type);
    setCloudProviderName(cloudProviderName);
    setClusterName(clusterName);
    setNamespace(namespace);
    setReleaseName(releaseName);
    setResourceGroup(resourceGroup);
    setSubscriptionId(subscriptionId);
  }

  public AzureKubernetesServiceYaml() {
    super(AZURE_KUBERNETES);
  }
}
