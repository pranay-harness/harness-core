package software.wings.helpers.ext.helm.response;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.cloudprovider.ContainerInfo;

import java.util.List;

/**
 * Created by anubhaw on 4/2/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class HelmInstallCommandResponse extends HelmCommandResponse {
  private List<ContainerInfo> containerInfoList;

  @Builder
  public HelmInstallCommandResponse(
      CommandExecutionStatus commandExecutionStatus, String output, List<ContainerInfo> containerInfoList) {
    super(commandExecutionStatus, output);
    this.containerInfoList = containerInfoList;
  }
}
