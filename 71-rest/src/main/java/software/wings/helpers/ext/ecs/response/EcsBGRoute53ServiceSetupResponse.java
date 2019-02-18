package software.wings.helpers.ext.ecs.response;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;

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