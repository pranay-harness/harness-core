package software.wings.core.ssh.executors;

import org.junit.Test;
import software.wings.core.ssh.executors.SSHSessionConfig.SSHSessionConfigBuilder;

/**
 * Created by anubhaw on 2/10/16.
 */
public class SSHPwdAuthExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHSessionConfig config =
        new SSHSessionConfigBuilder().host("localhost").port(3333).user("osboxes").password("osboxes.org").build();

    SSHExecutor executor = new SSHPwdAuthExecutor();
    executor.init(config);
    executor.execute("seq 100000 > numbers && sort numbers | wc -l && rm numbers");
    //
    //        Thread thread1 = new Thread(()->{
    //            System.out.println("Sorting numbers");
    //            try {
    //                executor.execute("seq 10000000 > numbers && sort numbers | sort | sort |wc -l && rm numbers");
    //                System.out.println("Sorting completed");
    //            } catch (Exception ex) {
    //                System.out.println("Sorting failed: " + ex.getStackTrace());
    //            }
    //        });
    //
    //        Thread thread2 = new Thread(()->{
    //            try {
    //                System.out.println("Stop sorting");
    //                executor.abort();
    //            } catch (Exception e) {
    //                e.printStackTrace();
    //            }
    //            System.out.println("Sorting Stopped");
    //        });
    //
    //        System.out.println("start threads");
    //        thread1.setName("Sorting thread");
    //        thread1.start();
    //        Thread.sleep(2000);
    //        thread2.setName("Abort thread");
    //        thread2.start();
    //        Thread.sleep(1000000);
  }
}