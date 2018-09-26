package software.wings.core;

import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.List;

public interface BaseExecutor {
  CommandExecutionStatus executeCommandString(String command);

  CommandExecutionStatus executeCommandString(String command, boolean displayCommand);

  CommandExecutionStatus executeCommandString(String command, StringBuffer output);

  CommandExecutionStatus executeCommandString(String command, StringBuffer output, boolean displayCommand);

  CommandExecutionStatus copyConfigFiles(ConfigFileMetaData configFileMetaData);

  CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files);

  CommandExecutionStatus copyFiles(String destinationDirectoryPath, ArtifactStreamAttributes artifactStreamAttributes,
      String accountId, String appId, String activityId, String commandUnitName, String hostName);

  CommandExecutionStatus copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds);

  CommandExecutionStatus copyGridFsFiles(ConfigFileMetaData configFileMetaData);
}
