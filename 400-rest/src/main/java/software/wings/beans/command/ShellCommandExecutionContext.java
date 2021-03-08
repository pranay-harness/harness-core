package software.wings.beans.command;

import io.harness.delegate.service.DelegateAgentFileService.FileBucket;
import io.harness.logging.CommandExecutionStatus;
import io.harness.shell.BaseScriptExecutor;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;
import software.wings.core.ssh.executors.FileBasedScriptExecutor;

import java.util.List;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.tuple.Pair;

@EqualsAndHashCode(callSuper = true)
public class ShellCommandExecutionContext extends CommandExecutionContext {
  private BaseScriptExecutor executor;
  private FileBasedScriptExecutor fileBasedScriptExecutor;

  public ShellCommandExecutionContext(CommandExecutionContext other) {
    super(other);
  }

  public CommandExecutionStatus copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds) {
    return fileBasedScriptExecutor.copyGridFsFiles(
        evaluateVariable(destinationDirectoryPath), fileBucket, fileNamesIds);
  }

  public CommandExecutionStatus copyConfigFiles(ConfigFileMetaData configFileMetaData) {
    configFileMetaData.setDestinationDirectoryPath(evaluateVariable(configFileMetaData.getDestinationDirectoryPath()));
    return fileBasedScriptExecutor.copyConfigFiles(configFileMetaData);
  }

  public CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files) {
    return fileBasedScriptExecutor.copyFiles(evaluateVariable(destinationDirectoryPath), files);
  }

  public CommandExecutionStatus copyFiles(String destinationDirectoryPath,
      ArtifactStreamAttributes artifactStreamAttributes, String accountId, String appId, String activityId,
      String commandUnitName, String hostName) {
    return fileBasedScriptExecutor.copyFiles(evaluateVariable(destinationDirectoryPath), artifactStreamAttributes,
        accountId, appId, activityId, commandUnitName, hostName);
  }

  public CommandExecutionStatus executeCommandString(String commandString) {
    return executor.executeCommandString(commandString, false);
  }

  public CommandExecutionStatus executeCommandString(String commandString, boolean displayCommand) {
    return executor.executeCommandString(commandString, displayCommand);
  }

  public CommandExecutionStatus executeCommandString(String commandString, StringBuffer output) {
    return executor.executeCommandString(commandString, output);
  }

  public void setExecutor(BaseScriptExecutor executor) {
    this.executor = executor;
  }

  public void setFileBasedScriptExecutor(FileBasedScriptExecutor fileBasedScriptExecutor) {
    this.fileBasedScriptExecutor = fileBasedScriptExecutor;
  }
}
