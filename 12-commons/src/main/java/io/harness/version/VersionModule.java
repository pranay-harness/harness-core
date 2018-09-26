package io.harness.version;

import com.google.inject.AbstractModule;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class VersionModule extends AbstractModule {
  @Override
  protected void configure() {
    try {
      VersionInfoManager versionInfoManager = new VersionInfoManager(IOUtils.toString(
          this.getClass().getClassLoader().getResourceAsStream("versionInfo.yaml"), StandardCharsets.UTF_8));
      bind(VersionInfoManager.class).toInstance(versionInfoManager);
    } catch (IOException e) {
      throw new RuntimeException("Could not load versionInfo.yaml", e);
    }
  }
}
