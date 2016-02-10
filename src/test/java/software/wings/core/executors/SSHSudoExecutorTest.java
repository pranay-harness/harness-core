package software.wings.core.executors;

import org.junit.Test;
import software.wings.core.executors.callbacks.ConsoleExecutionCallback;

/**
 * Created by anubhaw on 2/5/16.
 */

public class SSHSudoExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHSudoCommandExecutor executor = new SSHSudoCommandExecutor("localhost", 2222, "osboxes", "osboxes.org",
        "sudo su - vagrant  -c 'ls && whoami'", new ConsoleExecutionCallback(null));
    executor.execute();
  }
}