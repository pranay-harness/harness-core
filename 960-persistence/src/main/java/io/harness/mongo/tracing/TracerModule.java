package io.harness.mongo.tracing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.PIPELINE)
public class TracerModule extends AbstractModule {
  private static TracerModule instance;

  public static TracerModule getInstance() {
    if (instance == null) {
      instance = new TracerModule();
    }
    return instance;
  }

  @Override
  public void configure() {}
}
