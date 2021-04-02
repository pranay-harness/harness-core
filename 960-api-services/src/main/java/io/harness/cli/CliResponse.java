package io.harness.cli;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class CliResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
  private String error;
}
