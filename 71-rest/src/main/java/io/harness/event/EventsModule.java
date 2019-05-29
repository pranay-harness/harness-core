package io.harness.event;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import io.harness.event.handler.EventHandler;
import io.harness.event.handler.impl.MarketoHandler;
import io.harness.event.handler.impl.NateroEventHandler;
import io.harness.event.handler.impl.VerificationEventHandler;
import io.harness.event.handler.impl.notifications.AlertNotificationHandler;
import io.harness.event.handler.impl.segment.SegmentHandler;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.event.listener.EventListener;
import io.harness.event.publisher.EventPublisher;
import io.harness.event.usagemetrics.HarnessMetricsRegistryHandler;
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

    MapBinder<String, EventHandler> eventHandlerMapBinder =
        MapBinder.newMapBinder(binder(), String.class, EventHandler.class);
    if (mainConfiguration.getMarketoConfig() != null) {
      bind(MarketoConfig.class).toInstance(mainConfiguration.getMarketoConfig());
      eventHandlerMapBinder.addBinding("MarketoHandler")
          .toInstance(new MarketoHandler(mainConfiguration.getMarketoConfig(), eventListener));
    }

    if (mainConfiguration.getSegmentConfig() != null) {
      bind(SegmentConfig.class).toInstance(mainConfiguration.getSegmentConfig());
      eventHandlerMapBinder.addBinding("SegmentHandler")
          .toInstance(new SegmentHandler(mainConfiguration.getSegmentConfig(), eventListener));
    }

    eventHandlerMapBinder.addBinding(AlertNotificationHandler.class.getSimpleName())
        .toInstance(new AlertNotificationHandler(eventListener));

    HarnessMetricsRegistryHandler harnessMetricsRegistryHandler = new HarnessMetricsRegistryHandler();
    eventHandlerMapBinder.addBinding("HarnessMetricsRegistryHandler").toInstance(harnessMetricsRegistryHandler);

    eventHandlerMapBinder.addBinding("VerificationEventListner")
        .toInstance(new VerificationEventHandler(eventListener));

    eventHandlerMapBinder.addBinding("NateroEventHandler").toInstance(new NateroEventHandler(eventListener));
  }
}
