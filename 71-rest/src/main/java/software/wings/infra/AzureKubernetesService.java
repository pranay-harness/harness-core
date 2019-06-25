package software.wings.infra;

import lombok.Data;

@Data
public class AzureKubernetesService implements KubernetesInfrastructure, CloudProviderInfrastructure {
  private String cloudProviderId;
  private String clusterName;
  private String namespace;
  private String releaseName;
  private String subscriptionId;
  private String resourceGroup;
}
