package software.wings.core.executors;

import org.junit.Test;

/**
 * Created by anubhaw on 2/5/16.
 */

public class SSHSudoExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHSessionConfig config = new SSHSessionConfig.SSHSessionConfigBuilder()
                                  .setHost("localhost")
                                  .setPort(3333)
                                  .setUser("osboxes")
                                  .setPassword("osboxes.org")
                                  .setSudoUserName("vagrant")
                                  .setSudoUserPassword("wings1234")
                                  .build();

    SSHExecutor executor = new SSHSudoExecutor();
    executor.init(config);
    executor.execute("sudo su - vagrant  -c 'ls && whoami'");
  }
}