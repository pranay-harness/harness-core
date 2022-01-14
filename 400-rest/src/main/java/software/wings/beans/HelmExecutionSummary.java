package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.container.ContainerInfo;
import io.harness.delegate.task.helm.HelmChartInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class HelmExecutionSummary {
  private HelmChartInfo helmChartInfo;
  private String releaseName;
  private List<ContainerInfo> containerInfoList;
}
