package io.harness.version;

import io.harness.serializer.YamlUtils;

import java.io.IOException;

public class VersionInfoManager {
  private final VersionInfo versionInfo;

  public VersionInfoManager(String versionInfoYaml) {
    try {
      this.versionInfo = new YamlUtils().read(versionInfoYaml, VersionInfo.class);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Failed to parse yaml content %s", versionInfoYaml), e);
    }
  }

  public VersionInfo getVersionInfo() {
    return this.versionInfo;
  }
}
