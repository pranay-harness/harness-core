package io.harness;

import com.google.inject.AbstractModule;

public class EventsFrameworkModule extends AbstractModule {
  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;

  public EventsFrameworkModule(EventsFrameworkConfiguration configuration) {
    this.eventsFrameworkConfiguration = configuration;
  }

  @Override
  protected void configure() {}
}
