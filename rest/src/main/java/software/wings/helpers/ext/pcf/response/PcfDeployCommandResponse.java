package software.wings.helpers.ext.pcf.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.PcfInstanceElement;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class PcfDeployCommandResponse extends PcfCommandResponse {
  /**
   * This list represents apps updated by deploy state,
   * AppName : previousCount : DesiredCount (one updated by deply)
   * Rollback will use this data but will reverse counts
   */
  private List<PcfServiceData> instanceDataUpdated;
  private List<PcfInstanceElement> pcfInstanceElements;

  @Builder
  public PcfDeployCommandResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<PcfServiceData> instanceDataUpdated, List<PcfInstanceElement> pcfInstanceElements) {
    super(commandExecutionStatus, output);
    this.instanceDataUpdated = instanceDataUpdated;
    this.pcfInstanceElements = pcfInstanceElements;
  }
}
