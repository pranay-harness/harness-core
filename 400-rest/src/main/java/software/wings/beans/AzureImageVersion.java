package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzureImageVersion {
  private String name;
  private String imageDefinitionName;
  private String subscriptionId;
  private String resourceGroupName;
  private String location;
  private String galleryName;
}
