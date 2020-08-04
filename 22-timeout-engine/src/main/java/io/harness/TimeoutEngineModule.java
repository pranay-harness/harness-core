package io.harness;

import com.google.inject.AbstractModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeoutEngineModule extends AbstractModule {
  private static TimeoutEngineModule instance;

  public static TimeoutEngineModule getInstance() {
    if (instance == null) {
      instance = new TimeoutEngineModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    // Nothing to configure.
  }
}
