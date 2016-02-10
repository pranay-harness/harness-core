package software.wings.core.executors;

import org.junit.Test;
import software.wings.core.executors.callbacks.ConsoleExecutionCallback;

/**
 * Created by anubhaw on 2/5/16.
 */
public class SSHJumpBoxExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHJumpBoxCommandExecutor executor = new SSHJumpBoxCommandExecutor(
        "192.168.1.14", 22, "osboxes", "osboxes.org", "ls && whoami", new ConsoleExecutionCallback(null));
    executor.execute();
  }
}