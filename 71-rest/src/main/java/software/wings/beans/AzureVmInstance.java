package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
public class AzureVmInstance extends AzureResourceReference {
  @Builder
  private AzureVmInstance(String name, String resourceGroup, String subscriptionId, String type, String id,
      String publicIpAddress, String publicDnsName) {
    super(name, resourceGroup, subscriptionId, type, id);
    this.publicIpAddress = publicIpAddress;
    this.publicDnsName = publicDnsName;
  }

  private String publicIpAddress;
  private String publicDnsName;
}
