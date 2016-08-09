package software.wings.core.ssh.executors;

import software.wings.beans.command.CommandUnit.ExecutionResult;
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
  ExecutionResult executeCommandString(String command);

  /**
   * Execute command string execution result.
   *
   * @param command the command
   * @param output  the output
   * @return the execution result
   */
  ExecutionResult executeCommandString(String command, StringBuffer output);

  /**
   * Scp grid fs files execution result.
   *
   * @param destinationDirectoryPath the destination directory path
   * @param fileBucket               the file bucket
   * @param gridFsFileId             the grid fs file id
   * @return the execution result
   */
  ExecutionResult copyGridFsFiles(String destinationDirectoryPath, FileBucket fileBucket, List<String> gridFsFileId);

  /**
   * Scp files execution result.
   *
   * @param destinationDirectoryPath the destination directory path
   * @param files                    the files
   * @return the execution result
   */
  ExecutionResult copyFiles(String destinationDirectoryPath, List<String> files);

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
