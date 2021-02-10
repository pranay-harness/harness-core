package software.wings.helpers.ext.ecs.response;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
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
