package software.wings.helpers.ext.ecs.response;

import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsBGRoute53ServiceSetupResponse extends EcsCommandResponse {
  private ContainerSetupCommandUnitExecutionData setupData;

  @Builder
  public EcsBGRoute53ServiceSetupResponse(
      CommandExecutionStatus commandExecutionStatus, String output, ContainerSetupCommandUnitExecutionData setupData) {
    super(commandExecutionStatus, output);
    this.setupData = setupData;
  }
}
