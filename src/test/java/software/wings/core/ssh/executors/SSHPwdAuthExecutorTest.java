package software.wings.core.ssh.executors;

import org.junit.Test;
import software.wings.core.ssh.executors.SSHExecutor.ExecutionResult;
import software.wings.core.ssh.executors.SSHSessionConfig.SSHSessionConfigBuilder;

import static software.wings.common.UUIDGenerator.getUUID;

/**
 * Created by anubhaw on 2/10/16.
 */
public class SSHPwdAuthExecutorTest {
  private String host = "192.168.1.56";
  private Integer port = 22;
  private String user = "osboxes";
  private String password = "osboxes.org";
  private String keyPath = "/Users/anubhaw/.ssh/wings";

  @Test
  public void testExecuteFailureScenarios() throws Exception {
    SSHSessionConfig config = new SSHSessionConfigBuilder()
                                  .host(host)
                                  .port(port)
                                  .user(user)
                                  //                        .password(password)
                                  .keyPath(keyPath)
                                  .keyPassphrase("wings123")
                                  .build();

    SSHExecutor executor = new SSHPwdAuthExecutor();
    executor.init(config);
    executor.execute("ls && whoami");
  }

  @Test
  public void testSCP() throws Exception {
    SSHSessionConfig config = new SSHSessionConfig.SSHSessionConfigBuilder()
                                  .executionID(getUUID())
                                  .SSHConnectionTimeout(100000)
                                  .SSHSessionTimeout(100000)
                                  .host("192.168.43.163")
                                  .port(22)
                                  .user("osboxes")
                                  .password("osboxes.org")
                                  .build();

    SSHExecutor executor = SSHExecutorFactory.getExecutor(config);
    String fileName = "mvim";
    ExecutionResult result = executor.transferFile("/Users/anubhaw/Downloads/" + fileName, "./" + fileName);
    System.out.println(result);
  }

  @Test
  public void testExecute() throws Exception {
    SSHSessionConfig config =
        new SSHSessionConfigBuilder().host("localhost").port(3333).user("osboxes").password("osboxes.org").build();

    SSHExecutor executor = SSHExecutorFactory.getExecutor(config);
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