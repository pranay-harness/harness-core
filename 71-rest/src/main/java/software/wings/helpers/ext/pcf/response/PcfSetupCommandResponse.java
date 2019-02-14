package software.wings.helpers.ext.pcf.response;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * This class represents response from PcfCommandTask.SETP
 * It returns guid for new application created, name and
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PcfSetupCommandResponse extends PcfCommandResponse {
  private PcfAppSetupTimeDetails newApplicationDetails;
  private Integer totalPreviousInstanceCount;
  private List<PcfAppSetupTimeDetails> downsizeDetails;
  @Builder
  public PcfSetupCommandResponse(CommandExecutionStatus commandExecutionStatus, String output,
      PcfAppSetupTimeDetails newApplicationDetails, Integer totalPreviousInstanceCount,
      List<PcfAppSetupTimeDetails> downsizeDetails) {
    super(commandExecutionStatus, output);
    this.newApplicationDetails = newApplicationDetails;
    this.totalPreviousInstanceCount = totalPreviousInstanceCount;
    this.downsizeDetails = downsizeDetails;
  }
}
