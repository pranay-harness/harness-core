package io.harness.event.app;

import com.google.inject.AbstractModule;

import io.harness.event.grpc.EventServiceAuth;
import io.harness.grpc.auth.AuthService;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;

public class EventServiceModule extends AbstractModule {
  private final EventServiceConfig eventServiceConfig;

  public EventServiceModule(EventServiceConfig eventServiceConfig) {
    this.eventServiceConfig = eventServiceConfig;
  }

  @Override
  protected void configure() {
    bind(EventServiceConfig.class).toInstance(eventServiceConfig);
    bind(AuthService.class).to(EventServiceAuth.class);
    bind(HPersistence.class).to(MongoPersistence.class);
  }
}
