package io.harness.delegate.task.helm;

import io.harness.container.ContainerInfo;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
public class HelmRollbackCommandResponseNG extends HelmCommandResponseNG {
  private List<ContainerInfo> containerInfoList;

  @Builder
  public HelmRollbackCommandResponseNG(
      CommandExecutionStatus commandExecutionStatus, String output, List<ContainerInfo> containerInfoList) {
    super(commandExecutionStatus, output);
    this.containerInfoList = containerInfoList;
  }
}
