package software.wings.helpers.ext.k8s.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class K8sCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
