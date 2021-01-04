package io.harness.yaml;

import com.google.inject.AbstractModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YamlSdkModule extends AbstractModule {
  private static volatile YamlSdkModule defaultInstance;

  public static YamlSdkModule getInstance() {
    if (defaultInstance == null) {
      defaultInstance = new YamlSdkModule();
    }
    return defaultInstance;
  }

  private YamlSdkModule() {}

  @Override
  protected void configure() {}
}
