package io.harness.shell;

import io.harness.logging.CommandExecutionStatus;

import java.util.List;

public interface BaseScriptExecutor {
  CommandExecutionStatus executeCommandString(String command);

  CommandExecutionStatus executeCommandString(String command, boolean displayCommand);

  CommandExecutionStatus executeCommandString(String command, StringBuffer output);

  CommandExecutionStatus executeCommandString(String command, StringBuffer output, boolean displayCommand);

  ExecuteCommandResponse executeCommandString(String command, List<String> envVariablesToCollect);
}
