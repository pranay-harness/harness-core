package software.wings.core.ssh.executors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.io.File;

public class SshSessionFactoryTest {
  @Test
  public void getKeyPath() {
    SshSessionConfig config = SshSessionConfig.Builder.aSshSessionConfig().build();
    String homeDir = System.getProperty("user.home");
    String expectedString = homeDir + File.separator + ".ssh" + File.separator + "id_rsa";
    assertThat(SshSessionFactory.getKeyPath(config)).isEqualTo(expectedString);

    config = SshSessionConfig.Builder.aSshSessionConfig().withKeyPath("ExpectedKeyPath").build();
    assertThat(SshSessionFactory.getKeyPath(config)).isEqualTo("ExpectedKeyPath");
  }
}
