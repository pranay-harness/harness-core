package software.wings.helpers.ext.pcf;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.WingsBaseTest;

import java.util.concurrent.TimeUnit;

public class PivotalClientTest extends WingsBaseTest {
  PcfClientImpl pcfClient = new PcfClientImpl();

  @Test
  @Category(UnitTests.class)
  public void testHandlePasswordForSpecialCharacters() throws Exception {
    String password = "Ab1~!@#$%^&*()_'\"c";
    ProcessExecutor processExecutor =
        new ProcessExecutor()
            .timeout(1, TimeUnit.MINUTES)
            .command(
                "/bin/sh", "-c", new StringBuilder(128).append("echo ").append(password).append(" | cat ").toString());
    ProcessResult processResult = processExecutor.execute();
    assertThat(processResult.getExitValue()).isNotEqualTo(0);

    password = pcfClient.handlePwdForSpecialCharsForShell("Ab1~!@#$%^&*()_'\"c");
    processExecutor = new ProcessExecutor()
                          .timeout(1, TimeUnit.MINUTES)
                          .command("/bin/sh", "-c",
                              new StringBuilder(128).append("echo ").append(password).append(" | cat ").toString());
    processResult = processExecutor.execute();
    assertThat(processResult.getExitValue()).isEqualTo(0);
  }
}
