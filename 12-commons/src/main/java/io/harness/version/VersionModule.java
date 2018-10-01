package io.harness.version;

import io.harness.govern.DependencyModule;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class VersionModule extends DependencyModule {
  private static VersionModule instance;

  public static VersionModule getInstance() {
    if (instance == null) {
      instance = new VersionModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    String versionInfo = "version   : 0.0.0.0\n"
        + "buildNo   : 0.0\n"
        + "gitCommit : 0000000\n"
        + "gitBranch : unknown\n"
        + "timestamp : 000000-0000";

    try {
      final InputStream stream = this.getClass().getClassLoader().getResourceAsStream("versionInfo.yaml");
      if (stream != null) {
        versionInfo = IOUtils.toString(stream, StandardCharsets.UTF_8);
      }
    } catch (IOException ignore) {
      // Do nothing
    }
    bind(VersionInfoManager.class).toInstance(new VersionInfoManager(versionInfo));
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }
}
