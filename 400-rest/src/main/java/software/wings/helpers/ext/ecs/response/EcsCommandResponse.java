package software.wings.helpers.ext.ecs.response;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.logging.CommandExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class EcsCommandResponse implements DelegateResponseData {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
  private boolean timeoutFailure;
}
