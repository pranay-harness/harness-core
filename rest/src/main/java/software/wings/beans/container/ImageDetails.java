package software.wings.beans.container;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageDetails {
  private String name;
  private String tag;
  private String sourceName;
  private String registryUrl;
  private String username;
  private String password;
}
