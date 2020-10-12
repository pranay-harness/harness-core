package software.wings.core.ssh.executors;

import static io.harness.rule.OwnerRule.RUSHABH;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.core.ssh.executors.SshSessionFactory.getCopyOfKey;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;

public class SshSessionFactoryTest extends CategoryTest {
  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void getKeyPath() {
    SshSessionConfig config = SshSessionConfig.Builder.aSshSessionConfig().build();
    String homeDir = System.getProperty("user.home");
    String expectedString = homeDir + File.separator + ".ssh" + File.separator + "id_rsa";
    assertThat(SshSessionFactory.getKeyPath(config)).isEqualTo(expectedString);

    config = SshSessionConfig.Builder.aSshSessionConfig().withKeyPath("ExpectedKeyPath").build();
    assertThat(SshSessionFactory.getKeyPath(config)).isEqualTo("ExpectedKeyPath");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void cloneKey() {
    char[] src = "--BEGIN PRIVATE KEY abc --".toCharArray();
    char[] copySrc = getCopyOfKey(SshSessionConfig.Builder.aSshSessionConfig().withKey(src).build());
    assertThat(String.valueOf(copySrc)).isEqualTo("--BEGIN PRIVATE KEY abc --");
    assertThat(copySrc != src);
  }
}
