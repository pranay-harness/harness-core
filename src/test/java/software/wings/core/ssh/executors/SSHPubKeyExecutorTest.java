package software.wings.core.ssh.executors;

import org.junit.Test;
import software.wings.core.ssh.executors.SshSessionConfig.SshSessionConfigBuilder;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SSHPubKeyExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SshSessionConfig config = new SshSessionConfigBuilder()
                                  .host("192.168.1.88")
                                  .port(22)
                                  .user("osboxes")
                                  .keyPath("/Users/anubhaw/.ssh/id_rsa")
                                  //                        .keyPassphrase("wings123")
                                  .build();

    SshExecutor executor = SSHExecutorFactory.getExecutor(config);
    //        executor.execute("seq 100000 > numbers && sort numbers | wc -l && rm numbers");
    executor.execute("ld && whoami");
  }
}
