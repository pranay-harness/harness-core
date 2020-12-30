package software.wings.core.ssh.executors;

import io.harness.delegate.service.DelegateAgentFileService;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.CopyConfigCommandUnit;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public interface FileBasedScriptExecutor {
  CommandExecutionStatus copyConfigFiles(CopyConfigCommandUnit.ConfigFileMetaData configFileMetaData);

  CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files);

  CommandExecutionStatus copyFiles(String destinationDirectoryPath, ArtifactStreamAttributes artifactStreamAttributes,
      String accountId, String appId, String activityId, String commandUnitName, String hostName);

  CommandExecutionStatus copyGridFsFiles(String destinationDirectoryPath,
      DelegateAgentFileService.FileBucket fileBucket, List<Pair<String, String>> fileNamesIds);
}
