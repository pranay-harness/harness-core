/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.handler.EventHandler;
import io.harness.event.handler.impl.MarketoHandler;
import io.harness.event.handler.impl.VerificationEventHandler;
import io.harness.event.handler.impl.account.AccountChangeHandler;
import io.harness.event.handler.impl.notifications.AlertNotificationHandler;
import io.harness.event.handler.impl.segment.SalesforceApiCheck;
import io.harness.event.handler.impl.segment.SegmentHandler;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.event.handler.segment.SalesforceConfig;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.event.listener.EventListener;
import io.harness.event.publisher.EventPublisher;
import io.harness.event.timeseries.TimeSeriesHandler;
import io.harness.event.usagemetrics.HarnessMetricsRegistryHandler;

import software.wings.app.MainConfiguration;
import software.wings.service.impl.event.GenericEventListener;
import software.wings.service.impl.event.GenericEventPublisher;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import javax.annotation.Nullable;

/**
 * Guice Module for initializing events framework classes.
 * @author rktummala on 11/26/18
 */
@OwnedBy(PL)
public class EventsModule extends AbstractModule {
  private MainConfiguration mainConfiguration;

  public EventsModule(MainConfiguration mainConfiguration) {
    this.mainConfiguration = mainConfiguration;
  }

  @Provides
  @Singleton
  public SalesforceConfig salesforceConfig(MainConfiguration mainConfiguration) {
    return mainConfiguration.getSalesforceConfig();
  }

  @Provides
  @Singleton
  public SalesforceApiCheck salesforceApiCheck(Injector injector, SalesforceConfig salesforceConfig) {
    if (salesforceConfig == null) {
      return null;
    }
    final SalesforceApiCheck salesforceApiCheck = new SalesforceApiCheck(salesforceConfig);
    injector.injectMembers(salesforceApiCheck);
    return salesforceApiCheck;
  }

  @Provides
  @Singleton
  @Named("GenericEventListener")
  public EventListener eventListener(GenericEventListener eventListener) {
    return eventListener;
  }

  @Provides
  @Singleton
  public MarketoConfig marketoConfig(MainConfiguration mainConfiguration) {
    return mainConfiguration.getMarketoConfig();
  }

  @Provides
  @Singleton
  public MarketoHandler marketoHandler(
      Injector injector, @Nullable MarketoConfig marketoConfig, GenericEventListener eventListener) {
    if (marketoConfig == null) {
      return null;
    }
    final MarketoHandler marketoHandler = new MarketoHandler(marketoConfig, eventListener);
    injector.injectMembers(marketoHandler);
    return marketoHandler;
  }

  @Provides
  @Singleton
  public SegmentConfig segmentConfig(MainConfiguration mainConfiguration) {
    return mainConfiguration.getSegmentConfig();
  }

  @Provides
  @Singleton
  public SegmentHandler segmentHandler(
      Injector injector, @Nullable SegmentConfig segmentConfig, GenericEventListener eventListener) {
    if (segmentConfig == null) {
      return null;
    }
    final SegmentHandler segmentHandler = new SegmentHandler(segmentConfig, eventListener);
    injector.injectMembers(segmentHandler);
    return segmentHandler;
  }

  @Provides
  @Singleton
  public AlertNotificationHandler alertNotificationHandler(Injector injector, GenericEventListener eventListener) {
    final AlertNotificationHandler alertNotificationHandler = new AlertNotificationHandler(eventListener);
    injector.injectMembers(alertNotificationHandler);
    return alertNotificationHandler;
  }

  @Provides
  @Singleton
  public AccountChangeHandler accountChangeHandler(Injector injector, GenericEventListener eventListener) {
    final AccountChangeHandler accountChangeHandler = new AccountChangeHandler(eventListener);
    injector.injectMembers(accountChangeHandler);
    return accountChangeHandler;
  }

  @Provides
  @Singleton
  public HarnessMetricsRegistryHandler harnessMetricsRegistryHandler(Injector injector) {
    final HarnessMetricsRegistryHandler harnessMetricsRegistryHandler = new HarnessMetricsRegistryHandler();
    injector.injectMembers(harnessMetricsRegistryHandler);
    return harnessMetricsRegistryHandler;
  }

  @Provides
  @Singleton
  public VerificationEventHandler verificationEventHandler(Injector injector, GenericEventListener eventListener) {
    final VerificationEventHandler verificationEventHandler = new VerificationEventHandler(eventListener);
    injector.injectMembers(verificationEventHandler);
    return verificationEventHandler;
  }

  @Provides
  @Singleton
  public TimeSeriesHandler timeSeriesHandler(Injector injector, GenericEventListener eventListener) {
    final TimeSeriesHandler timeSeriesHandler = new TimeSeriesHandler(eventListener);
    injector.injectMembers(timeSeriesHandler);
    return timeSeriesHandler;
  }

  @Override
  protected void configure() {
    bind(EventPublisher.class).to(GenericEventPublisher.class);

    MapBinder<String, EventHandler> eventHandlerMapBinder =
        MapBinder.newMapBinder(binder(), String.class, EventHandler.class);
    if (mainConfiguration.getMarketoConfig() != null) {
      eventHandlerMapBinder.addBinding(MarketoHandler.class.getSimpleName()).to(MarketoHandler.class);
    }

    SegmentConfig segmentConfig = mainConfiguration.getSegmentConfig();
    if (segmentConfig != null) {
      eventHandlerMapBinder.addBinding(SegmentHandler.class.getSimpleName()).to(SegmentHandler.class);
    }

    eventHandlerMapBinder.addBinding(AlertNotificationHandler.class.getSimpleName()).to(AlertNotificationHandler.class);
    eventHandlerMapBinder.addBinding(AccountChangeHandler.class.getSimpleName()).to(AccountChangeHandler.class);
    eventHandlerMapBinder.addBinding(HarnessMetricsRegistryHandler.class.getSimpleName())
        .to(HarnessMetricsRegistryHandler.class);
    eventHandlerMapBinder.addBinding(VerificationEventHandler.class.getSimpleName()).to(VerificationEventHandler.class);
    eventHandlerMapBinder.addBinding(TimeSeriesHandler.class.getSimpleName()).to(TimeSeriesHandler.class);
  }
}
