package software.wings.helpers.ext.ecs.response;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.logging.CommandExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EcsCommandResponse implements DelegateResponseData {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
  private boolean timeoutFailure;
}
