import org.junit.Test;

import software.wings.helpers.SSHCommandExecutor;

public class SSHCommandExecutorTest {
  @Test
  public void test() {
    SSHCommandExecutor executor =
        new SSHCommandExecutor("localhost", 2222, "vagrant", "wings123", "ls -al && whoami && pwd", null);
    executor.execute();
  }
}
