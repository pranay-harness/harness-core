package io.harness;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;

import io.harness.config.PublisherConfiguration;
import io.harness.delay.DelayEvent;
import io.harness.delay.DelayEventListener;
import io.harness.engine.events.OrchestrationEventListener;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.mongo.queue.QueueFactory;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.queue.QueuePublisher;
import io.harness.version.VersionInfoManager;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class OrchestrationQueueModule extends AbstractModule {
  private static OrchestrationQueueModule instance;

  public static synchronized OrchestrationQueueModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationQueueModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueueListener<DelayEvent>>() {}).to(DelayEventListener.class);
    bind(new TypeLiteral<QueueListener<OrchestrationEvent>>() {}).to(OrchestrationEventListener.class);
  }

  @Provides
  @Singleton
  QueuePublisher<DelayEvent> delayQueuePublisher(
      Injector injector, VersionInfoManager versionInfoManager, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(
        injector, DelayEvent.class, singletonList(versionInfoManager.getVersionInfo().getVersion()), config);
  }

  @Provides
  @Singleton
  QueueConsumer<DelayEvent> delayQueueConsumer(
      Injector injector, VersionInfoManager versionInfoManager, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, DelayEvent.class, ofSeconds(5),
        singletonList(singletonList(versionInfoManager.getVersionInfo().getVersion())), config);
  }

  @Provides
  @Singleton
  QueuePublisher<OrchestrationEvent> orchestrationEventQueuePublisher(
      Injector injector, VersionInfoManager versionInfoManager, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(
        injector, OrchestrationEvent.class, singletonList(versionInfoManager.getVersionInfo().getVersion()), config);
  }

  @Provides
  @Singleton
  QueueConsumer<OrchestrationEvent> orchestrationEventQueueConsumer(
      Injector injector, VersionInfoManager versionInfoManager, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, OrchestrationEvent.class, ofSeconds(5),
        singletonList(singletonList(versionInfoManager.getVersionInfo().getVersion())), config);
  }
}
