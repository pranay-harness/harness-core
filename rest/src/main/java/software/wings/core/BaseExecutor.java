package software.wings.core;

import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.List;

public interface BaseExecutor {
  CommandExecutionStatus executeCommandString(String command);

  CommandExecutionStatus executeCommandString(String command, StringBuffer output);

  CommandExecutionStatus copyConfigFiles(ConfigFileMetaData configFileMetaData);

  CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files);

  CommandExecutionStatus copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds);

  CommandExecutionStatus copyGridFsFiles(ConfigFileMetaData configFileMetaData);
}
