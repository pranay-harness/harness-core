package io.harness;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;

import io.harness.config.PublisherConfiguration;
import io.harness.delay.DelayEvent;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrchestrationDelayModule extends AbstractModule {
  private static OrchestrationDelayModule instance;

  public static OrchestrationDelayModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationDelayModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueueListener<io.harness.delay.DelayEvent>>() {
    }).to(io.harness.delay.DelayEventListener.class);
  }

  @Provides
  @Singleton
  QueuePublisher<io.harness.delay.DelayEvent> delayQueuePublisher(
      Injector injector, VersionInfoManager versionInfoManager, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, io.harness.delay.DelayEvent.class,
        singletonList(versionInfoManager.getVersionInfo().getVersion()), config);
  }

  @Provides
  @Singleton
  QueueConsumer<DelayEvent> delayQueueConsumer(
      Injector injector, VersionInfoManager versionInfoManager, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, io.harness.delay.DelayEvent.class, ofSeconds(5),
        singletonList(singletonList(versionInfoManager.getVersionInfo().getVersion())), config);
  }
}
