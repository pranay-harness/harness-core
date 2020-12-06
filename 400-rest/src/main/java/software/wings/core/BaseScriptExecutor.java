package software.wings.core;

import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.service.DelegateAgentFileService.FileBucket;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public interface BaseScriptExecutor {
  CommandExecutionStatus executeCommandString(String command);

  CommandExecutionStatus executeCommandString(String command, boolean displayCommand);

  CommandExecutionStatus executeCommandString(String command, StringBuffer output);

  CommandExecutionStatus executeCommandString(String command, StringBuffer output, boolean displayCommand);

  CommandExecutionResult executeCommandString(String command, List<String> envVariablesToCollect);

  CommandExecutionStatus copyConfigFiles(ConfigFileMetaData configFileMetaData);

  CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files);

  CommandExecutionStatus copyFiles(String destinationDirectoryPath, ArtifactStreamAttributes artifactStreamAttributes,
      String accountId, String appId, String activityId, String commandUnitName, String hostName);

  CommandExecutionStatus copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds);
}
