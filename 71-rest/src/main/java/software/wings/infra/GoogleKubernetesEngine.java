package software.wings.infra;

import lombok.Data;

@Data
public class GoogleKubernetesEngine implements KubernetesInfrastructure, CloudProviderInfrastructure {
  private String cloudProviderId;
  private String clusterName;
  private String namespace;
  private String releaseName;
}
