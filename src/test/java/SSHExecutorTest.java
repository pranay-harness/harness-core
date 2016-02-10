import org.junit.Test;

import software.wings.core.executors.SSHCommandExecutor;
import software.wings.core.executors.callbacks.ConsoleExecutionCallback;
import software.wings.core.executors.SSHSudoCommandExecutor;

public class SSHExecutorTest {
  @Test
  public void test() {
    SSHCommandExecutor executor = new SSHCommandExecutor(
        "localhost", 2222, "osboxes", "osboxes.org", "ls && whoami", new ConsoleExecutionCallback(null));
    executor.execute();
  }
}
