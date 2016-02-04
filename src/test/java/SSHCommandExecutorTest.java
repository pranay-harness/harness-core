import org.junit.Test;

import software.wings.helpers.ConsoleExecutionCallback;
import software.wings.helpers.SSHCommandExecutor;

public class SSHCommandExecutorTest {
  @Test
  public void test() {
    SSHCommandExecutor executor = new SSHCommandExecutor(
        "localhost", 2222, "vagrant", "wings1234", "ls -al && whoami && pwd", new ConsoleExecutionCallback(null));
    executor.execute();
  }
}
