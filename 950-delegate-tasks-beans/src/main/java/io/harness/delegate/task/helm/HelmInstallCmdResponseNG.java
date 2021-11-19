package io.harness.delegate.task.helm;

import io.harness.container.ContainerInfo;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
public class HelmInstallCmdResponseNG extends HelmCommandResponseNG {
  private List<ContainerInfo> containerInfoList;
  private HelmChartInfo helmChartInfo;

  @Builder
  public HelmInstallCmdResponseNG(CommandExecutionStatus commandExecutionStatus, String output,
      List<ContainerInfo> containerInfoList, HelmChartInfo helmChartInfo) {
    super(commandExecutionStatus, output);
    this.containerInfoList = containerInfoList;
    this.helmChartInfo = helmChartInfo;
  }
}
