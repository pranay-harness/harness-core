package software.wings.helpers.ext.pcf.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.List;

/**
 * This class represents response from PcfCommandTask.SETP
 * It returns guid for new application created, name and
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PcfSetupCommandResponse extends PcfCommandResponse {
  private String newApplicationId;
  private String newApplicationName;
  private Integer totalPreviousInstanceCount;
  private List<String> downsizeDetails;
  @Builder
  public PcfSetupCommandResponse(CommandExecutionStatus commandExecutionStatus, String output, String newApplicationId,
      String newApplicationName, Integer totalPreviousInstanceCount, List<String> downsizeDetails) {
    super(commandExecutionStatus, output);
    this.newApplicationId = newApplicationId;
    this.newApplicationName = newApplicationName;
    this.totalPreviousInstanceCount = totalPreviousInstanceCount;
    this.downsizeDetails = downsizeDetails;
  }
}
