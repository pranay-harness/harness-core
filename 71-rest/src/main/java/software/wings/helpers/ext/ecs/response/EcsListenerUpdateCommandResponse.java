package software.wings.helpers.ext.ecs.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsListenerUpdateCommandResponse extends EcsCommandResponse {
  private String downsizedServiceName;
  private int downsizedServiceCount;

  @Builder
  public EcsListenerUpdateCommandResponse(CommandExecutionStatus commandExecutionStatus, String output,
      String downsizedServiceName, int downsizedServiceCount) {
    super(commandExecutionStatus, output);
    this.downsizedServiceName = downsizedServiceName;
    this.downsizedServiceCount = downsizedServiceCount;
  }
}
