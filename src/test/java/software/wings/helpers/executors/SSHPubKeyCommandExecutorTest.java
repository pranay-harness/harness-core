package software.wings.helpers.executors;

import org.junit.Test;
import software.wings.helpers.executors.callbacks.ConsoleExecutionCallback;

import static org.junit.Assert.*;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SSHPubKeyCommandExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHPubKeyCommandExecutor executor = new SSHPubKeyCommandExecutor(
        "192.168.1.13", 22, "osboxes", "PASSWORD_NOT_REQUIRED", "ls && whoami", new ConsoleExecutionCallback(null));
    executor.execute();
  }
}