package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
public class AzureAvailabilitySet extends AzureResourceReference {
  @Builder
  private AzureAvailabilitySet(String name, String resourceGroup, String subscriptionId, String type, String id) {
    super(name, resourceGroup, subscriptionId, type, id);
  }
}
