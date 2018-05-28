package software.wings.beans.command;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;
import software.wings.core.BaseExecutor;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.List;

/**
 * Created by peeyushaggarwal on 8/2/16.
 */
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class ShellCommandExecutionContext extends CommandExecutionContext {
  private BaseExecutor executor;

  public ShellCommandExecutionContext(CommandExecutionContext other) {
    super(other);
  }

  public CommandExecutionStatus copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds) {
    return executor.copyGridFsFiles(evaluateVariable(destinationDirectoryPath), fileBucket, fileNamesIds);
  }

  public CommandExecutionStatus copyConfigFiles(ConfigFileMetaData configFileMetaData) {
    configFileMetaData.setDestinationDirectoryPath(evaluateVariable(configFileMetaData.getDestinationDirectoryPath()));
    return executor.copyConfigFiles(configFileMetaData);
  }

  public CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files) {
    return executor.copyFiles(evaluateVariable(destinationDirectoryPath), files);
  }

  public CommandExecutionStatus executeCommandString(String commandString) {
    return executor.executeCommandString(commandString);
  }

  public CommandExecutionStatus executeCommandString(String commandString, StringBuffer output) {
    return executor.executeCommandString(commandString, output);
  }

  public void setExecutor(BaseExecutor executor) {
    this.executor = executor;
  }
}
