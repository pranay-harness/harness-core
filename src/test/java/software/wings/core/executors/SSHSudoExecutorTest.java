package software.wings.core.executors;

import org.junit.Test;

/**
 * Created by anubhaw on 2/5/16.
 */

public class SSHSudoExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHSessionConfig config = new SSHSessionConfig("localhost", 3333, "osboxes", "osboxes.org", "vagrant", "wings1234");
    SSHExecutor executor = new SSHSudoExecutor();
    executor.init(config);
    executor.execute("sudo su - vagrant  -c 'ls && whoami'");
  }
}