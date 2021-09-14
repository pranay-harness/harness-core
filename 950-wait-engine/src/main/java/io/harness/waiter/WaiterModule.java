/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import static java.util.Arrays.asList;

import io.harness.TimeoutEngineModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.config.PublisherConfiguration;
import io.harness.mongo.queue.QueueFactory;
import io.harness.queue.QueueModule;
import io.harness.queue.QueuePublisher;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.WaiterConfiguration.PersistenceLayer;
import io.harness.waiter.persistence.MorphiaPersistenceWrapper;
import io.harness.waiter.persistence.PersistenceWrapper;
import io.harness.waiter.persistence.SpringPersistenceWrapper;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.DEL)
public class WaiterModule extends AbstractModule {
  private static WaiterModule instance;

  public static WaiterModule getInstance() {
    if (instance == null) {
      instance = new WaiterModule();
    }
    return instance;
  }

  @Provides
  @Singleton
  QueuePublisher<NotifyEvent> notifyQueuePublisher(Injector injector, VersionInfoManager versionInfoManager,
      PublisherConfiguration config, WaiterConfiguration waiterConfiguration) {
    if (waiterConfiguration.getPersistenceLayer() == PersistenceLayer.MORPHIA) {
      return QueueFactory.createQueuePublisher(
          injector, NotifyEvent.class, asList(versionInfoManager.getVersionInfo().getVersion()), config);
    } else {
      return QueueFactory.createNgQueuePublisher(injector, NotifyEvent.class,
          asList(versionInfoManager.getVersionInfo().getVersion()), config, injector.getInstance(MongoTemplate.class));
    }
  }

  @Provides
  @Singleton
  PersistenceWrapper persistenceWrapper(Injector injector, WaiterConfiguration waiterConfiguration) {
    if (waiterConfiguration.getPersistenceLayer() == PersistenceLayer.MORPHIA) {
      return injector.getInstance(MorphiaPersistenceWrapper.class);
    } else {
      return injector.getInstance(SpringPersistenceWrapper.class);
    }
  }

  @Override
  protected void configure() {
    install(QueueModule.getInstance());
    install(TimeoutEngineModule.getInstance());
  }
}
