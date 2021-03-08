package software.wings.beans.command;

import io.harness.container.ContainerInfo;
import io.harness.shell.CommandExecutionData;

import software.wings.api.ContainerServiceData;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class ResizeCommandUnitExecutionData implements CommandExecutionData {
  private List<ContainerInfo> containerInfos;
  private List<ContainerInfo> previousContainerInfos;
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;
  private String namespace;

  // Change made to support newInstanceAPI for CV
  @Singular private List<ContainerInfo> allContainerInfos;
}
