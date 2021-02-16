package software.wings.helpers.ext.helm.response;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by anubhaw on 3/22/18.
 */
@Data
@AllArgsConstructor
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class HelmCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
