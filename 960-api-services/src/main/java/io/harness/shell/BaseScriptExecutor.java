/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.shell;

import io.harness.logging.CommandExecutionStatus;

import java.util.List;

public interface BaseScriptExecutor {
  CommandExecutionStatus executeCommandString(String command);

  CommandExecutionStatus executeCommandString(String command, boolean displayCommand);

  CommandExecutionStatus executeCommandString(String command, StringBuffer output);

  CommandExecutionStatus executeCommandString(String command, StringBuffer output, boolean displayCommand);

  ExecuteCommandResponse executeCommandString(String command, List<String> envVariablesToCollect);

  ExecuteCommandResponse executeCommandString(
      String command, List<String> envVariablesToCollect, List<String> secretEnvVariablesToCollect);
}
