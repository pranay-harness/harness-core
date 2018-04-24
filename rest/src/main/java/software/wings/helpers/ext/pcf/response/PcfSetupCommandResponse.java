package software.wings.helpers.ext.pcf.response;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

/**
 * This class represents response from PcfCommandTask.SETP
 * It returns guid for new application created, name and
 */
@Data
public class PcfSetupCommandResponse extends PcfCommandResponse {
  private String newApplicationId;
  private String newApplicationName;
  private Integer totalPreviousInstanceCount;
  @Builder
  public PcfSetupCommandResponse(CommandExecutionStatus commandExecutionStatus, String output, String newApplicationId,
      String newApplicationName, Integer totalPreviousInstanceCount) {
    super(commandExecutionStatus, output);
    this.newApplicationId = newApplicationId;
    this.newApplicationName = newApplicationName;
    this.totalPreviousInstanceCount = totalPreviousInstanceCount;
  }
}
