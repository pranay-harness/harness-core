package software.wings.core.ssh.executors;

import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.command.CommandExecutionResult.AbstractCommandUnit.CommandExecutionStatus;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.List;
import javax.validation.Valid;

/**
 * Created by anubhaw on 2/4/16.
 */
public interface SshExecutor {
  /**
   * Inits the.
   *
   * @param config the config
   */
  void init(@Valid SshSessionConfig config);

  /**
   * Execute command string execution result.
   *
   * @param command the command
   * @return the execution result
   */
  CommandExecutionStatus executeCommandString(String command);

  /**
   * Execute command string execution result.
   *
   * @param command the command
   * @param output  the output
   * @return the execution result
   */
  CommandExecutionStatus executeCommandString(String command, StringBuffer output);

  /**
   * Scp grid fs files execution result.
   *
   * @param destinationDirectoryPath the destination directory path
   * @param fileBucket               the file bucket
   * @param fileNamesIds             the grid fs file id
   * @return the execution result
   */
  CommandExecutionStatus copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds);

  /**
   * Scp files execution result.
   *
   * @param destinationDirectoryPath the destination directory path
   * @param files                    the files
   * @return the execution result
   */
  CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files);

  /**
   * The Enum ExecutorType.
   */
  enum ExecutorType {
    /**
     * Password auth executor type.
     */
    PASSWORD_AUTH, /**
                    * Key auth executor type.
                    */
    KEY_AUTH, /**
               * Bastion host executor type.
               */
    BASTION_HOST
  }
}
