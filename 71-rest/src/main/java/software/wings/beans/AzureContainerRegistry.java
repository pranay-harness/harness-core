package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureContainerRegistry extends AzureResourceReference {
  @Builder
  private AzureContainerRegistry(
      String name, String resourceGroup, String subscriptionId, String type, String id, String loginServer) {
    super(name, resourceGroup, subscriptionId, type, id);
    this.loginServer = loginServer;
  };

  private String loginServer;
}
