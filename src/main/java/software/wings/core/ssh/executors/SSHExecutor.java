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

  public enum ExecutorType { PASSWORD, SSHKEY, JUMPBOX }

  public enum ExecutionResult { SUCCESS, FAILURE }
}
