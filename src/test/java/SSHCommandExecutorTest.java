import org.junit.Test;

import software.wings.helpers.executors.SSHCommandExecutor;
import software.wings.helpers.executors.callbacks.ConsoleExecutionCallback;
import software.wings.helpers.executors.SSHSudoCommandExecutor;

public class SSHCommandExecutorTest {
  @Test
  public void test() {
    SSHCommandExecutor executor = new SSHCommandExecutor(
        "localhost", 2222, "osboxes", "osboxes.org", "ls && whoami", new ConsoleExecutionCallback(null));
    executor.execute();
  }
}
