package software.wings.beans.command;

import io.harness.delegate.command.CommandExecutionData;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import software.wings.api.ContainerServiceData;
import software.wings.cloudprovider.ContainerInfo;

import java.util.List;

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
