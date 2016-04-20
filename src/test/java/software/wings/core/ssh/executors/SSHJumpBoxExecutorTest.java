package software.wings.core.ssh.executors;

import static software.wings.common.UUIDGenerator.getUUID;

import org.junit.Test;
import software.wings.core.ssh.executors.SSHSessionConfig.SSHSessionConfigBuilder;

/**
 * Created by anubhaw on 2/5/16.
 */
public class SSHJumpBoxExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHSessionConfig jumpboxConfig = new SSHSessionConfigBuilder()
                                         .executionID(getUUID())
                                         .host("192.168.43.163")
                                         .port(22)
                                         .user("osboxes")
                                         .password("osboxes.org")
                                         .build();

    SSHSessionConfig config = new SSHSessionConfigBuilder()
                                  .executionID(getUUID())
                                  .host("192.168.43.8")
                                  .port(22)
                                  .user("vagrant")
                                  .password("wings1234")
                                  .jumpboxConfig(jumpboxConfig)
                                  .build();

    SSHExecutor executor = SSHExecutorFactory.getExecutor(config);
    //        executor.execute("ls && whoami");
    String fileName = "mvim";
    SSHExecutor.ExecutionResult result = executor.transferFile("/Users/anubhaw/Downloads/" + fileName, "./" + fileName);
    System.out.println(result);
  }
}
