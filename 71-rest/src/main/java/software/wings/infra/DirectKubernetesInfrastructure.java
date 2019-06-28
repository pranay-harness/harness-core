package software.wings.infra;

import io.harness.data.validator.Trimmed;
import lombok.Data;

@Data
public class DirectKubernetesInfrastructure implements KubernetesInfrastructure, CloudProviderInfrastructure {
  private String cloudProviderId;
  private String clusterName;
  private String namespace;
  @Trimmed private String releaseName;
}
