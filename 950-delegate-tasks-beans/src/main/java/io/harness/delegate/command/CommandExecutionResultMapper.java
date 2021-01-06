package io.harness.delegate.command;

import io.harness.shell.ExecuteCommandResponse;

import javax.annotation.Nonnull;

public class CommandExecutionResultMapper {
  public static CommandExecutionResult from(@Nonnull ExecuteCommandResponse executeCommandResponse) {
    return CommandExecutionResult.builder()
        .commandExecutionData(executeCommandResponse.getCommandExecutionData())
        .status(executeCommandResponse.getStatus())
        .build();
  }
}
