package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class AzureAvailabilitySet extends AzureResourceReference {
  @Builder
  private AzureAvailabilitySet(String name, String resourceGroup, String subscriptionId, String type, String id) {
    super(name, resourceGroup, subscriptionId, type, id);
  }
}
