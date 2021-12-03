package io.harness.delegate.task.helm;

import io.harness.logging.CommandExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HelmCommandResponseNG {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
