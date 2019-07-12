package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

@JsonTypeName("AZURE_KUBERNETES")
@Data
public class AzureKubernetesService implements KubernetesInfrastructure, CloudProviderInfrastructure {
  private String cloudProviderId;
  private String clusterName;
  private String namespace;
  private String releaseName;
  private String subscriptionId;
  private String resourceGroup;
}
