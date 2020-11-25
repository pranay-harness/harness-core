package software.wings.delegatetasks.helm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HelmChartDetails {
  private String name;
  private String version;
  @JsonProperty("app_version") private String appVersion;
  private String description;
}
