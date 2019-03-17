package software.wings.core.ssh.executors;

import io.harness.category.element.UnitTests;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by anubhaw on 2/8/16.
 */
@Ignore
public class SshPubKeyExecutorTest {
  /**
   * Test execute.
   *
   * @throws Exception the exception
   */
  @Test
  @Category(UnitTests.class)
  public void testExecute() throws Exception {
    //    SshSessionConfig config =
    //        new SshSessionConfigBuilder().host("192.168.1.88").port(22).user("osboxes")
    //            .keyPath("/Users/anubhaw/.ssh/id_rsa")
    //            //                        .keyPassphrase("wings123")
    //            .build();
    //
    //    SshExecutor executor = SshExecutorFactory.getExecutor(config);
    //    //        executor.execute("seq 100000 > numbers && sort numbers | wc -l && rm numbers");
    //    executor.execute("ld && whoami");
  }
}
