package software.wings.core.ssh.executors;

/**
 * Created by anubhaw on 2/4/16.
 */
public interface SshExecutor {
  void init(SshSessionConfig config);

  ExecutionResult execute(String command);

  ExecutionResult transferFile(String localFilePath, String remoteFilePath);

  void abort();

  void destroy();

  enum ExecutorType { PASSWORD, SSHKEY, JUMPBOX }

  enum ExecutionResult { SUCCESS, FAILURE }
}
