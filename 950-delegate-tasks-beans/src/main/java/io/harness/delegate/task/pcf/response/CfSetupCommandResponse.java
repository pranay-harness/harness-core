package io.harness.delegate.task.pcf.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.task.pcf.CfCommandResponse;
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
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class CfSetupCommandResponse extends CfCommandResponse {
  private CfAppSetupTimeDetails newApplicationDetails;
  private Integer totalPreviousInstanceCount;
  private List<CfAppSetupTimeDetails> downsizeDetails;
  private Integer instanceCountForMostRecentVersion;
  private CfAppSetupTimeDetails mostRecentInactiveAppVersion;

  @Builder
  public CfSetupCommandResponse(CommandExecutionStatus commandExecutionStatus, String output,
      CfAppSetupTimeDetails newApplicationDetails, Integer totalPreviousInstanceCount,
      List<CfAppSetupTimeDetails> downsizeDetails, Integer instanceCountForMostRecentVersion,
      CfAppSetupTimeDetails mostRecentInactiveAppVersion) {
    super(commandExecutionStatus, output);
    this.newApplicationDetails = newApplicationDetails;
    this.totalPreviousInstanceCount = totalPreviousInstanceCount;
    this.downsizeDetails = downsizeDetails;
    this.instanceCountForMostRecentVersion = instanceCountForMostRecentVersion;
    this.mostRecentInactiveAppVersion = mostRecentInactiveAppVersion;
  }
}
