package io.harness.version;

import io.harness.serializer.YamlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

public class VersionInfoManager {
  private static final String INIT_VERSION_INFO = "version   : 0.0.0.0\n"
      + "buildNo   : 0.0\n"
      + "gitCommit : 0000000\n"
      + "gitBranch : unknown\n"
      + "timestamp : 000000-0000";
  private static final String VERSION_INFO_YAML = "main/resources-filtered/versionInfo.yaml";

  private final VersionInfo versionInfo;

  private String fullVersion;

  private static String initVersionInfo() {
    String versionInfo = VersionInfoManager.INIT_VERSION_INFO;

    try {
      final InputStream stream = VersionInfoManager.class.getClassLoader().getResourceAsStream(VERSION_INFO_YAML);
      if (stream != null) {
        versionInfo = IOUtils.toString(stream, StandardCharsets.UTF_8);
      }
    } catch (IOException ignore) {
      // Do nothing
    }
    return versionInfo;
  }

  public VersionInfoManager() {
    this(initVersionInfo());
  }

  public VersionInfoManager(String versionInfoYaml) {
    try {
      this.versionInfo = new YamlUtils().read(versionInfoYaml, VersionInfo.class);
      fullVersion = versionInfo.getVersion() + "-" + versionInfo.getPatch();
    } catch (IOException e) {
      throw new RuntimeException(String.format("Failed to parse yaml content %s", versionInfoYaml), e);
    }
  }
  public String getFullVersion() {
    return fullVersion;
  }

  public VersionInfo getVersionInfo() {
    return this.versionInfo;
  }
}
