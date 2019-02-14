package software.wings.helpers.ext.helm.response;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by anubhaw on 3/22/18.
 */
@Data
@AllArgsConstructor
public class HelmCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
