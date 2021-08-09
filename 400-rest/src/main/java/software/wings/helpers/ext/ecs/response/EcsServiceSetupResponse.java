package software.wings.helpers.ext.ecs.response;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class EcsServiceSetupResponse extends EcsCommandResponse {
  private boolean isBlueGreen;
  private ContainerSetupCommandUnitExecutionData setupData;

  @Builder
  public EcsServiceSetupResponse(CommandExecutionStatus commandExecutionStatus, String output, boolean isBlueGreen,
      ContainerSetupCommandUnitExecutionData setupData, boolean timeoutFailure) {
    super(commandExecutionStatus, output, timeoutFailure);
    this.isBlueGreen = isBlueGreen;
    this.setupData = setupData;
  }
}
