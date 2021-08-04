package io.harness.mongo.tracing;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class TracerModule extends AbstractModule {
  private static TracerModule instance;
  public static Module getInstance() {
    if (instance == null) {
      instance = new TracerModule();
    }
    return instance;
  }

  @Override
  public void configure() {}
}
