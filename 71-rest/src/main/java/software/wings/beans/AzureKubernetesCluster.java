package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
public class AzureKubernetesCluster extends AzureResourceReference {
  @Builder
  private AzureKubernetesCluster(String name, String resourceGroup, String subscriptionId, String type, String id) {
    super(name, resourceGroup, subscriptionId, type, id);
  };
}
