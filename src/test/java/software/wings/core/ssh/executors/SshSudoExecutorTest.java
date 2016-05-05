package software.wings.core.ssh.executors;

import static software.wings.common.UUIDGenerator.getUuid;

import org.junit.Ignore;
import org.junit.Test;

import software.wings.core.ssh.executors.SshSessionConfig.SshSessionConfigBuilder;

/**
 * Created by anubhaw on 2/5/16.
 */
@Ignore
public class SshSudoExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SshSessionConfig config = new SshSessionConfigBuilder()
                                  .executionId(getUuid())
                                  .host("localhost")
                                  .port(2222)
                                  .user("osboxes")
                                  .password("osboxes.org")
                                  .sudoUserName("vagrant")
                                  .sudoUserPassword("osboxes.org")
                                  .build();

    SshExecutor executor = SshExecutorFactory.getExecutor(config);
    executor.execute("sudo su - vagrant  -c 'ls && whoami'");
  }
}
