package software.wings.core.ssh.executors;

import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.service.intfc.FileService.FileBucket;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 2/4/16.
 */
public interface SshExecutor {
  /**
   * Inits the.
   *
   * @param config the config
   */
  void init(SshSessionConfig config);

  /**
   * Execute.
   *
   * @param command the command
   * @return the execution result
   */
  ExecutionResult execute(String command);

  /**
   * Transfer file.
   *
   * @param gridFsFileId   the grid fs file id
   * @param remoteFilePath the remote file path
   * @param fileBucket     the file bucket
   * @return the execution result
   */
  ExecutionResult transferFile(String gridFsFileId, String remoteFilePath, FileBucket fileBucket);

  /**
   * Abort.
   */
  void abort();

  /**
   * Destroy.
   */
  void destroy();

  /**
   * The Enum ExecutorType.
   */
  enum ExecutorType { PASSWORD_AUTH, KEY_AUTH, BASTION_HOST }
}
