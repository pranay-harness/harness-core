package io.harness.delegate.task.helm;

import io.harness.container.ContainerInfo;
import io.harness.k8s.model.HelmVersion;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
public class HelmInstallCmdResponseNG extends HelmCommandResponseNG {
  private List<ContainerInfo> containerInfoList;
  private HelmChartInfo helmChartInfo;
  private int prevReleaseVersion;
  private String releaseName;
  private HelmVersion helmVersion;

  @Builder
  public HelmInstallCmdResponseNG(CommandExecutionStatus commandExecutionStatus, String output,
      List<ContainerInfo> containerInfoList, HelmChartInfo helmChartInfo, int prevReleaseVersion, String releaseName, HelmVersion helmVersion) {
    super(commandExecutionStatus, output);
    this.containerInfoList = containerInfoList;
    this.helmChartInfo = helmChartInfo;
    this.prevReleaseVersion = prevReleaseVersion;
    this.releaseName = releaseName;
    this.helmVersion = helmVersion;
  }
}
