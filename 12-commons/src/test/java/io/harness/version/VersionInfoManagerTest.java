package io.harness.version;

import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.PUNEET;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.YamlUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class VersionInfoManagerTest extends CategoryTest {
  private static String sampleVersionInfoYaml = "version   : 1.1.1.1\n"
      + "buildNo   : 1.0\n"
      + "gitCommit : 6f6df35\n"
      + "gitBranch : master\n"
      + "timestamp : 180621-0636";

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testGetVersionInfo() {
    VersionInfoManager versionInfoManager = new VersionInfoManager(sampleVersionInfoYaml);
    VersionInfo versionInfo = versionInfoManager.getVersionInfo();
    assertThat(versionInfo.getVersion()).isEqualTo("1.1.1.1");
    assertThat(versionInfo.getBuildNo()).isEqualTo("1.0");
    assertThat(versionInfo.getGitCommit()).isEqualTo("6f6df35");
    assertThat(versionInfo.getGitBranch()).isEqualTo("master");
    assertThat(versionInfo.getTimestamp()).isEqualTo("180621-0636");
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testGetVersionInfoDefault() throws Exception {
    InputStream stream = VersionInfoManager.class.getClassLoader().getResourceAsStream("versionInfo.yaml");
    String versionInfoString = IOUtils.toString(stream, StandardCharsets.UTF_8);
    VersionInfo versionInfo = new YamlUtils().read(versionInfoString, VersionInfo.class);
    VersionInfoManager versionInfoManager = new VersionInfoManager();
    assertThat(versionInfoManager.getVersionInfo()).isEqualTo(versionInfo);
  }
}
