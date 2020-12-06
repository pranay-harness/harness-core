package software.wings.helpers.ext.ecs.response;

import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsServiceSetupResponse extends EcsCommandResponse {
  private boolean isBlueGreen;
  private ContainerSetupCommandUnitExecutionData setupData;

  @Builder
  public EcsServiceSetupResponse(CommandExecutionStatus commandExecutionStatus, String output, boolean isBlueGreen,
      ContainerSetupCommandUnitExecutionData setupData) {
    super(commandExecutionStatus, output);
    this.isBlueGreen = isBlueGreen;
    this.setupData = setupData;
  }
}
