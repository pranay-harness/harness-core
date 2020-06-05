package io.harness.perpetualtask;

import com.google.inject.AbstractModule;

public class PerpetualTaskServiceModule extends AbstractModule {
  private static volatile PerpetualTaskServiceModule instance;

  public static PerpetualTaskServiceModule getInstance() {
    if (instance == null) {
      instance = new PerpetualTaskServiceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(PerpetualTaskService.class).to(PerpetualTaskServiceImpl.class);
  }
}