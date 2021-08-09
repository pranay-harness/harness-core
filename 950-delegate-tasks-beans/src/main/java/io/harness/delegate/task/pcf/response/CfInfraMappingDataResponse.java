package io.harness.delegate.task.pcf.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.pcf.CfCommandResponse;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CDP)
public class CfInfraMappingDataResponse extends CfCommandResponse {
  private List<String> organizations;
  private List<String> spaces;
  private List<String> routeMaps;
  private Integer runningInstanceCount;

  @Builder
  public CfInfraMappingDataResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<String> organizations, List<String> spaces, List<String> routeMaps, Integer runningInstanceCount) {
    super(commandExecutionStatus, output);
    this.organizations = organizations;
    this.spaces = spaces;
    this.routeMaps = routeMaps;
    this.runningInstanceCount = runningInstanceCount;
  }
}
