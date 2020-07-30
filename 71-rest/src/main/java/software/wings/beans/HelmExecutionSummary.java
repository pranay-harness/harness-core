package software.wings.beans;

import io.harness.container.ContainerInfo;
import lombok.Builder;
import lombok.Data;
import software.wings.helpers.ext.helm.response.HelmChartInfo;

import java.util.List;

@Data
@Builder
public class HelmExecutionSummary {
  private HelmChartInfo helmChartInfo;
  private String releaseName;
  private List<ContainerInfo> containerInfoList;
}
