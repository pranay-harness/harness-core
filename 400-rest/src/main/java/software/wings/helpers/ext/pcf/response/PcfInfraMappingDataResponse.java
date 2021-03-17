package software.wings.helpers.ext.pcf.response;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class PcfInfraMappingDataResponse extends PcfCommandResponse {
  private List<String> organizations;
  private List<String> spaces;
  private List<String> routeMaps;
  private Integer runningInstanceCount;

  @Builder
  public PcfInfraMappingDataResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<String> organizations, List<String> spaces, List<String> routeMaps, Integer runningInstanceCount) {
    super(commandExecutionStatus, output);
    this.organizations = organizations;
    this.spaces = spaces;
    this.routeMaps = routeMaps;
    this.runningInstanceCount = runningInstanceCount;
  }
}
