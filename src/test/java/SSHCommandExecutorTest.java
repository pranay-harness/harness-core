import org.junit.Test;

import software.wings.helpers.executors.callbacks.ConsoleExecutionCallback;
import software.wings.helpers.executors.SSHSudoCommandExecutor;

public class SSHCommandExecutorTest {
  @Test
  public void test() {
    SSHSudoCommandExecutor executor = new SSHSudoCommandExecutor("localhost", 2222, "osboxes", "osboxes.org",
        "sudo su - vagrant  -c 'ls && whoami'", new ConsoleExecutionCallback(null));
    executor.execute();
  }
}
