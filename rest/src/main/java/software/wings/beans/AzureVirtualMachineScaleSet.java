package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
public class AzureVirtualMachineScaleSet extends AzureResourceReference {
  @Builder
  private AzureVirtualMachineScaleSet(String name, String resourceGroup, String type, String id) {
    super(name, resourceGroup, type, id);
  }
}
