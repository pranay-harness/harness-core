package software.wings.helpers.ext.helm.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HelmChartInfo {
  private String name;
  private String version;
  private String repoUrl;
}
