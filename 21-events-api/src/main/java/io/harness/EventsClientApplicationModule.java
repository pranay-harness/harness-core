package io.harness;

import com.google.inject.AbstractModule;

public class EventsClientApplicationModule extends AbstractModule {
  private final EventsClientApplicationConfiguration appConfig;

  public EventsClientApplicationModule(EventsClientApplicationConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  protected void configure() {
    install(new EventsFrameworkModule(this.appConfig.getEventsFrameworkConfiguration()));
  }
}
