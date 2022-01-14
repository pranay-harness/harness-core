package io.harness.delegate.task.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@OwnedBy(CDP)
public class HelmCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
