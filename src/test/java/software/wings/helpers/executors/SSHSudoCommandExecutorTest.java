package software.wings.helpers.executors;

import org.junit.Test;
import software.wings.helpers.executors.callbacks.ConsoleExecutionCallback;

import static org.junit.Assert.*;

/**
 * Created by anubhaw on 2/5/16.
 */

public class SSHSudoCommandExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHSudoCommandExecutor executor = new SSHSudoCommandExecutor("localhost", 2222, "osboxes", "osboxes.org",
        "sudo su - vagrant  -c 'ls && whoami'", new ConsoleExecutionCallback(null));
    executor.execute();
  }
}