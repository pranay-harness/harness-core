package software.wings.core.executors;

import org.junit.Test;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SSHPubKeyExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHSessionConfig config = new SSHSessionConfig("localhost", 3333, "osboxes", "/Users/anubhaw/.ssh/id_rsa");
    SSHExecutor executor = new SSHPubKeyAuthExecutor();
    executor.init(config);
    executor.execute("seq 100000 > numbers && sort numbers | wc -l && rm numbers");
  }
}