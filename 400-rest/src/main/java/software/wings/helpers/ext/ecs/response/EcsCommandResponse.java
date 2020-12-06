package software.wings.helpers.ext.ecs.response;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.logging.CommandExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EcsCommandResponse implements DelegateResponseData {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
