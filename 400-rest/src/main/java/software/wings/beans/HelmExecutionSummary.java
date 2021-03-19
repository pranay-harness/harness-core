package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.container.ContainerInfo;

import software.wings.helpers.ext.helm.response.HelmChartInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class HelmExecutionSummary {
  private HelmChartInfo helmChartInfo;
  private String releaseName;
  private List<ContainerInfo> containerInfoList;
}
