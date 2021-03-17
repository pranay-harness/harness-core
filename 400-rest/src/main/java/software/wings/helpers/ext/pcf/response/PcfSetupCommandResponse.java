package software.wings.helpers.ext.pcf.response;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This class represents response from PcfCommandTask.SETP
 * It returns guid for new application created, name and
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class PcfSetupCommandResponse extends PcfCommandResponse {
  private PcfAppSetupTimeDetails newApplicationDetails;
  private Integer totalPreviousInstanceCount;
  private List<PcfAppSetupTimeDetails> downsizeDetails;
  private Integer instanceCountForMostRecentVersion;
  private PcfAppSetupTimeDetails mostRecentInactiveAppVersion;

  @Builder
  public PcfSetupCommandResponse(CommandExecutionStatus commandExecutionStatus, String output,
      PcfAppSetupTimeDetails newApplicationDetails, Integer totalPreviousInstanceCount,
      List<PcfAppSetupTimeDetails> downsizeDetails, Integer instanceCountForMostRecentVersion,
      PcfAppSetupTimeDetails mostRecentInactiveAppVersion) {
    super(commandExecutionStatus, output);
    this.newApplicationDetails = newApplicationDetails;
    this.totalPreviousInstanceCount = totalPreviousInstanceCount;
    this.downsizeDetails = downsizeDetails;
    this.instanceCountForMostRecentVersion = instanceCountForMostRecentVersion;
    this.mostRecentInactiveAppVersion = mostRecentInactiveAppVersion;
  }
}
