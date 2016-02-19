package software.wings.core.ssh.executors;

import org.junit.Test;
import software.wings.core.ssh.executors.SSHExecutor.ExecutionResult;

import static org.junit.Assert.*;
import static software.wings.common.UUIDGenerator.getUUID;

/**
 * Created by anubhaw on 2/18/16.
 */
public class SCPExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHSessionConfig config = new SSHSessionConfig.SSHSessionConfigBuilder()
                                  .executionID(getUUID())
                                  .SSHConnectionTimeout(100000)
                                  .SSHSessionTimeout(100000)
                                  .host("192.168.1.113")
                                  .port(22)
                                  .user("osboxes")
                                  .password("osboxes.org")
                                  .build();

    SCPExecutor executor = new SCPExecutor();
    executor.init(config);
    String fileName = "mvim";
    ExecutionResult result = executor.transfer("/Users/anubhaw/Downloads/" + fileName, "./" + fileName);
    System.out.println(result);
  }
}