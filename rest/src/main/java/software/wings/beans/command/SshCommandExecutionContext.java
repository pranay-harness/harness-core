package software.wings.beans.command;

import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.command.AbstractCommandUnit.ExecutionResult;
import software.wings.core.ssh.executors.SshExecutor;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.List;

/**
 * Created by peeyushaggarwal on 8/2/16.
 */
public class SshCommandExecutionContext extends CommandExecutionContext {
  private SshExecutor sshExecutor;

  /**
   * Instantiates a new Ssh command execution context.
   *
   * @param other the other
   */
  public SshCommandExecutionContext(CommandExecutionContext other) {
    super(other);
  }

  public ExecutionResult copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds) {
    return sshExecutor.copyGridFsFiles(evaluateVariable(destinationDirectoryPath), fileBucket, fileNamesIds);
  }

  public ExecutionResult copyFiles(String destinationDirectoryPath, List<String> files) {
    return sshExecutor.copyFiles(evaluateVariable(destinationDirectoryPath), files);
  }

  public ExecutionResult executeCommandString(String commandString) {
    return sshExecutor.executeCommandString(commandString);
  }

  public ExecutionResult executeCommandString(String commandString, StringBuffer output) {
    return sshExecutor.executeCommandString(commandString, output);
  }

  /**
   * Setter for property 'sshExecutor'.
   *
   * @param sshExecutor Value to set for property 'sshExecutor'.
   */
  public void setSshExecutor(SshExecutor sshExecutor) {
    this.sshExecutor = sshExecutor;
  }
}
