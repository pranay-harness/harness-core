package io.harness.app;

import io.harness.controller.PrimaryVersionController;
import io.harness.queue.QueueController;
import io.harness.version.VersionModule;

import com.google.inject.AbstractModule;

public class PrimaryVersionManagerModule extends AbstractModule {
  private static volatile PrimaryVersionManagerModule instance;

  private PrimaryVersionManagerModule() {}

  public static PrimaryVersionManagerModule getInstance() {
    if (instance == null) {
      instance = new PrimaryVersionManagerModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(VersionModule.getInstance());
    bind(QueueController.class).to(PrimaryVersionController.class);
  }
}
