package software.wings.core.executors;

import org.junit.Test;

/**
 * Created by anubhaw on 2/5/16.
 */

public class SSHSudoExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHSessionConfig config = new SSHSessionConfig.SSHSessionConfigBuilder()
                                  .host("localhost")
                                  .port(3333)
                                  .user("osboxes")
                                  .password("osboxes.org")
                                  .sudoUserName("vagrant")
                                  .sudoUserPassword("wings1234")
                                  .build();

    SSHExecutor executor = new SSHSudoExecutor();
    executor.init(config);
    executor.execute("sudo su - vagrant  -c 'ls && whoami'");
  }
}