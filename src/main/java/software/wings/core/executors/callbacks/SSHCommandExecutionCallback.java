package software.wings.core.executors.callbacks;

public interface SSHCommandExecutionCallback {
  public void log(String message);

  public void updateStatus();
}
