package software.wings.helpers.ext.helm.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

/**
 * Created by anubhaw on 3/22/18.
 */
@Data
@AllArgsConstructor
public class HelmCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
