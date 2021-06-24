package io.harness.delegate.task.pcf.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.pcf.CfInternalInstanceElement;
import io.harness.delegate.beans.pcf.CfServiceData;
import io.harness.delegate.task.pcf.CfCommandResponse;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class CfDeployCommandResponse extends CfCommandResponse {
  /**
   * This list represents apps updated by deploy state,
   * AppName : previousCount : DesiredCount (one updated by deploy)
   * Rollback will use this data but will reverse counts
   */
  private List<CfServiceData> instanceDataUpdated;
  private List<CfInternalInstanceElement> pcfInstanceElements;

  @Builder
  public CfDeployCommandResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<CfServiceData> instanceDataUpdated, List<CfInternalInstanceElement> pcfInstanceElements) {
    super(commandExecutionStatus, output);
    this.instanceDataUpdated = instanceDataUpdated;
    this.pcfInstanceElements = pcfInstanceElements;
  }
}
