package software.wings.helpers;

public interface SSHCommandExecutionCallback {
  public void log(String message);

  public void updateStatus();
}
