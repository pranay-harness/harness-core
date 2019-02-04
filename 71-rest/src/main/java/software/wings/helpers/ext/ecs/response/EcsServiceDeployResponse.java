package software.wings.helpers.ext.ecs.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.ContainerServiceData;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.sm.InstanceStatusSummary;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsServiceDeployResponse extends EcsCommandResponse {
  // TODO: revisit this class once refactoring completed
  private List<ContainerInfo> containerInfos;
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;
  private List<InstanceStatusSummary> newInstanceStatusSummaries;

  public EcsServiceDeployResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<ContainerInfo> containerInfos, List<ContainerServiceData> newInstanceData,
      List<ContainerServiceData> oldInstanceData, List<InstanceStatusSummary> newInstanceStatusSummaries) {
    super(commandExecutionStatus, output);
    this.containerInfos = containerInfos;
    this.newInstanceData = newInstanceData;
    this.oldInstanceData = oldInstanceData;
    this.newInstanceStatusSummaries = newInstanceStatusSummaries;
  }
}