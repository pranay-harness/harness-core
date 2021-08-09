package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AzureResourceReference {
  private String name;
  private String resourceGroup;
  private String subscriptionId;
  private String type;
  private String resourceId;
}
