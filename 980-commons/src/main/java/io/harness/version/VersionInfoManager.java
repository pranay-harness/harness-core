package io.harness.version;

import io.harness.project.TestUtils;
import io.harness.serializer.YamlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
public class VersionInfoManager {
  private static final String INIT_VERSION_INFO = "version   : 0.0.0.0\n"
      + "buildNo   : 0.0\n"
      + "patch     : 000\n"
      + "gitCommit : 0000000\n"
      + "gitBranch : unknown\n"
      + "timestamp : 000000-0000";
  private static final String VERSION_INFO_YAML = "io/harness/versionInfo.yaml";

  private final VersionInfo versionInfo;

  private String fullVersion;

  private static String initVersionInfo() {
    try (InputStream stream = VersionInfoManager.class.getClassLoader().getResourceAsStream(VERSION_INFO_YAML)) {
      return IOUtils.toString(stream, StandardCharsets.UTF_8);
    } catch (Exception exception) {
      if (TestUtils.isTestProcess()) {
        log.info("NOTE: Defaulting to test version information.\n"
            + "The resource is missing and the execution seems test releated.");
      } else {
        throw new RuntimeException("Failed to load version information", exception);
      }
    }
    return VersionInfoManager.INIT_VERSION_INFO;
  }

  public VersionInfoManager() {
    this(initVersionInfo());
  }

  public VersionInfoManager(String versionInfoYaml) {
    try {
      versionInfo = new YamlUtils().read(versionInfoYaml, VersionInfo.class);
      fullVersion = versionInfo.getVersion() + "-" + versionInfo.getPatch();
    } catch (IOException e) {
      throw new RuntimeException(String.format("Failed to parse yaml content %s", versionInfoYaml), e);
    }
  }

  public String getFullVersion() {
    return fullVersion;
  }

  public VersionInfo getVersionInfo() {
    return versionInfo;
  }
}
