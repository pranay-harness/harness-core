package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import software.wings.helpers.ext.helm.response.HelmChartInfo;

@Data
@Builder
public class HelmExecutionSummary {
  private HelmChartInfo helmChartInfo;
  private String releaseName;
}
