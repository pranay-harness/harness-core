package software.wings.helpers.ext.ecs.response;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.container.ContainerInfo;
import io.harness.logging.CommandExecutionStatus;

import software.wings.api.ContainerServiceData;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@OwnedBy(PL)
public class EcsServiceDeployResponse extends EcsCommandResponse {
  private List<ContainerInfo> containerInfos;
  private List<ContainerInfo> previousContainerInfos;
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;

  @Builder
  public EcsServiceDeployResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<ContainerInfo> containerInfos, List<ContainerServiceData> newInstanceData,
      List<ContainerServiceData> oldInstanceData, List<ContainerInfo> previousContainerInfos, boolean timeoutFailure) {
    super(commandExecutionStatus, output, timeoutFailure);
    this.containerInfos = containerInfos;
    this.newInstanceData = newInstanceData;
    this.oldInstanceData = oldInstanceData;
    this.previousContainerInfos = previousContainerInfos;
  }
}
