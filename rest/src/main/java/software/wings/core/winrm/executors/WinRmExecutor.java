package software.wings.core.winrm.executors;

import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.List;

public interface WinRmExecutor {
  CommandExecutionStatus executeCommandString(String command, StringBuffer output);
  CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files);
}
