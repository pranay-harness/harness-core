package software.wings.core.ssh.executors;

import static software.wings.common.UUIDGenerator.getUuid;

import org.junit.Ignore;
import org.junit.Test;

import software.wings.core.ssh.executors.SshSessionConfig.SshSessionConfigBuilder;

/**
 * Created by anubhaw on 2/5/16.
 */
@Ignore
public class SshJumpBoxExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SshSessionConfig jumpboxConfig = new SshSessionConfigBuilder()
                                         .executionId(getUuid())
                                         .host("192.168.43.163")
                                         .port(22)
                                         .user("osboxes")
                                         .password("osboxes.org")
                                         .build();

    SshSessionConfig config = new SshSessionConfigBuilder()
                                  .executionId(getUuid())
                                  .host("192.168.43.8")
                                  .port(22)
                                  .user("vagrant")
                                  .password("wings1234")
                                  .jumpboxConfig(jumpboxConfig)
                                  .build();

    SshExecutor executor = SshExecutorFactory.getExecutor(config);
    //        executor.execute("ls && whoami");
    String fileName = "mvim";
    SshExecutor.ExecutionResult result = executor.transferFile("/Users/anubhaw/Downloads/" + fileName, "./" + fileName);
    System.out.println(result);
  }
}
