package software.wings.helpers.executors;

/**
 * Created by anubhaw on 2/4/16.
 */
public interface CommandExecutor {
  Integer SSHConnectionTimeout = 10000; // 10 seconds
  Integer SSHSessionTimeout = 600000; // 10 minutes
  Integer RetryInterval = 1000;
  void execute();
}
