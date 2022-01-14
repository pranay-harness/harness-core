package software.wings.helpers.ext.helm.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SearchInfo {
  private String name;
  private String chartVersion;
  private String appVersion;
  private String description;
}
