package io.harness.event;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.event.handler.EventHandler;
import io.harness.event.handler.impl.MarketoHandler;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.event.listener.EventListener;
import io.harness.event.publisher.EventPublisher;
import software.wings.app.MainConfiguration;
import software.wings.service.impl.event.GenericEventListener;
import software.wings.service.impl.event.GenericEventPublisher;

/**
 * Guice Module for initializing events framework classes.
 * @author rktummala on 11/26/18
 */
public class EventsModule extends AbstractModule {
  private MainConfiguration mainConfiguration;

  public EventsModule(MainConfiguration mainConfiguration) {
    this.mainConfiguration = mainConfiguration;
  }

  @Override
  protected void configure() {
    GenericEventListener eventListener = new GenericEventListener();
    bind(EventListener.class).annotatedWith(Names.named("GenericEventListener")).toInstance(eventListener);
    bind(EventPublisher.class).to(GenericEventPublisher.class);
    if (mainConfiguration.getMarketoConfig() != null) {
      bind(MarketoConfig.class).toInstance(mainConfiguration.getMarketoConfig());
      bind(EventHandler.class)
          .annotatedWith(Names.named("MarketoHandler"))
          .toInstance(new MarketoHandler(mainConfiguration.getMarketoConfig(), eventListener));
    }
  }
}
