package software.wings.core.ssh.executors;

import org.junit.Test;
import software.wings.core.ssh.executors.SSHSessionConfig.SSHSessionConfigBuilder;

/**
 * Created by anubhaw on 2/5/16.
 */
public class SSHJumpBoxExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHSessionConfig jumpboxConfig =
        new SSHSessionConfigBuilder().host("192.168.1.56").port(22).user("osboxes").password("osboxes.org").build();

    SSHSessionConfig config = new SSHSessionConfigBuilder()
                                  .host("192.168.1.88")
                                  .port(22)
                                  .user("vagrant")
                                  .password("wings1234")
                                  .jumpboxConfig(jumpboxConfig)
                                  .build();

    SSHExecutor executor = new SSHJumpboxExecutor();
    executor.init(config);
    executor.execute("ls && whoami");
  }
}