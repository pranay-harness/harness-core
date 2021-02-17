package software.wings.helpers.ext.ecs.response;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.container.ContainerInfo;
import io.harness.logging.CommandExecutionStatus;

import software.wings.api.ContainerServiceData;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class EcsDeployRollbackDataFetchResponse extends EcsCommandResponse {
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;

  @Builder
  public EcsDeployRollbackDataFetchResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<ContainerInfo> containerInfos, List<ContainerServiceData> newInstanceData,
      List<ContainerServiceData> oldInstanceData, List<ContainerInfo> previousContainerInfos) {
    super(commandExecutionStatus, output);
    this.newInstanceData = newInstanceData;
    this.oldInstanceData = oldInstanceData;
  }
}
