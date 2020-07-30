package software.wings.helpers.ext.helm.response;

import io.harness.container.ContainerInfo;
import io.harness.logging.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Created by anubhaw on 4/2/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class HelmInstallCommandResponse extends HelmCommandResponse {
  private List<ContainerInfo> containerInfoList;
  private HelmChartInfo helmChartInfo;

  @Builder
  public HelmInstallCommandResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<ContainerInfo> containerInfoList, HelmChartInfo helmChartInfo) {
    super(commandExecutionStatus, output);
    this.containerInfoList = containerInfoList;
    this.helmChartInfo = helmChartInfo;
  }
}
