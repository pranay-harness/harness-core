package software.wings.beans.command;

import com.google.inject.assistedinject.AssistedInject;

import software.wings.beans.command.CommandUnit.ExecutionResult;
import software.wings.core.ssh.executors.SshExecutor;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.List;

/**
 * Created by peeyushaggarwal on 8/2/16.
 */
public class SshCommandExecutionContext extends CommandExecutionContext {
  private SshExecutor sshExecutor;

  @AssistedInject
  public SshCommandExecutionContext(CommandExecutionContext other) {
    super(other);
  }

  @Override
  public ExecutionResult copyGridFsFiles(String destinationDirectoryPath, FileBucket fileBucket, List<String> fileIds) {
    return sshExecutor.copyGridFsFiles(destinationDirectoryPath, fileBucket, fileIds);
  }

  @Override
  public ExecutionResult copyFiles(String destinationDirectoryPath, List<String> files) {
    return sshExecutor.copyFiles(destinationDirectoryPath, files);
  }

  @Override
  public ExecutionResult executeCommandString(String commandString) {
    return sshExecutor.executeCommandString(commandString);
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
