package software.wings.core.ssh.executors;

import org.junit.Test;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SSHPubKeyExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHSessionConfig config = new SSHSessionConfig.SSHSessionConfigBuilder()
                                  .host("192.168.1.88")
                                  .port(22)
                                  .user("osboxes")
                                  .keyPath("/Users/anubhaw/.ssh/id_rsa")
                                  //                        .keyPassphrase("wings123")
                                  .build();

    SSHExecutor executor = new SSHPubKeyAuthExecutor();
    executor.init(config);
    //        executor.execute("seq 100000 > numbers && sort numbers | wc -l && rm numbers");
    executor.execute("ld && whoami");
  }
}