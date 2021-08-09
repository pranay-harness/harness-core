package io.harness.shell;

import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExecuteCommandResponse {
  CommandExecutionStatus status;
  CommandExecutionData commandExecutionData;
}
