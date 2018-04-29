package software.wings.helpers.ext.pcf.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class PcfInfraMappingDataResponse extends PcfCommandResponse {
  private List<String> organizations;
  private List<String> spaces;
  private List<String> routeMaps;

  @Builder
  public PcfInfraMappingDataResponse(CommandExecutionStatus commandExecutionStatus, String output,
      List<String> organizations, List<String> spaces, List<String> routeMaps) {
    super(commandExecutionStatus, output);
    this.organizations = organizations;
    this.spaces = spaces;
    this.routeMaps = routeMaps;
  }
}
