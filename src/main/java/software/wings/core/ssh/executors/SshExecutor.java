package software.wings.core.ssh.executors;

import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.service.intfc.FileService.FileBucket;

/**
 * Created by anubhaw on 2/4/16.
 */
public interface SshExecutor {
  void init(SshSessionConfig config);

  ExecutionResult execute(String command);

  ExecutionResult transferFile(String gridFsFileId, String remoteFilePath, FileBucket fileBucket);

  void abort();

  void destroy();

  enum ExecutorType { PASSWORD_AUTH, KEY_AUTH, BASTION_HOST }
}
