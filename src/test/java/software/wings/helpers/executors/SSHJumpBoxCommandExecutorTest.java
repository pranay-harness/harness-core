package software.wings.helpers.executors;

import org.junit.Test;
import software.wings.helpers.executors.callbacks.ConsoleExecutionCallback;

import static org.junit.Assert.*;

/**
 * Created by anubhaw on 2/5/16.
 */
public class SSHJumpBoxCommandExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHJumpBoxCommandExecutor executor = new SSHJumpBoxCommandExecutor(
        "192.168.1.14", 22, "osboxes", "osboxes.org", "ls && whoami", new ConsoleExecutionCallback(null));
    executor.execute();
  }
}