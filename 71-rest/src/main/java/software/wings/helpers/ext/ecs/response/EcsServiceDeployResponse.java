package software.wings.helpers.ext.ecs.response;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.ContainerServiceData;
import software.wings.cloudprovider.ContainerInfo;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsServiceDeployResponse extends EcsCommandResponse {
  private List<ContainerInfo> containerInfos;
  private List<ContainerInfo> previousContainerInfos;
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;

  @Builder
  public EcsServiceDeployResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<ContainerInfo> containerInfos, List<ContainerServiceData> newInstanceData,
      List<ContainerServiceData> oldInstanceData, List<ContainerInfo> previousContainerInfos) {
    super(commandExecutionStatus, output);
    this.containerInfos = containerInfos;
    this.newInstanceData = newInstanceData;
    this.oldInstanceData = oldInstanceData;
    this.previousContainerInfos = previousContainerInfos;
  }
}