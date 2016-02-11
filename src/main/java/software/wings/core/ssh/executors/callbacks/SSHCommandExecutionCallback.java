package software.wings.core.ssh.executors.callbacks;

public interface SSHCommandExecutionCallback {
  public void log(String message);

  public void updateStatus();
}
