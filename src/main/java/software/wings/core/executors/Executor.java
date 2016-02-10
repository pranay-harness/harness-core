package software.wings.core.executors;

/**
 * Created by anubhaw on 2/4/16.
 */
public interface Executor {
  public enum ExecutorType { PASSWORD, SSHKEY, JUMPBOX }

  void init(SSHSessionConfig config);
  void execute(String command);
  void abort();
  void destroy();
}
