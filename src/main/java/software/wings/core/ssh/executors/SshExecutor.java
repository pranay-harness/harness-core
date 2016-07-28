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

  ExecutionResult executeCommandString(String command);

  ExecutionResult scpGridFsFiles(String destinationDirectoryPath, FileBucket fileBucket, List<String> gridFsFileId);

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
