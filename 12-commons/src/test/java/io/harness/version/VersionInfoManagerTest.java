package io.harness.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class VersionInfoManagerTest {
  private static String sampleVersionInfoYaml = "version   : 1.1.1.1\n"
      + "buildNo   : 1.0\n"
      + "gitCommit : 6f6df35\n"
      + "gitBranch : master\n"
      + "timestamp : 180621-0636";

  @Test
  public void testGetVersionInfo() {
    VersionInfoManager versionInfoManager = new VersionInfoManager(sampleVersionInfoYaml);
    VersionInfo versionInfo = versionInfoManager.getVersionInfo();
    assertThat(versionInfo.getVersion()).isEqualTo("1.1.1.1");
    assertThat(versionInfo.getBuildNo()).isEqualTo("1.0");
    assertThat(versionInfo.getGitCommit()).isEqualTo("6f6df35");
    assertThat(versionInfo.getGitBranch()).isEqualTo("master");
    assertThat(versionInfo.getTimestamp()).isEqualTo("180621-0636");
  }
}
