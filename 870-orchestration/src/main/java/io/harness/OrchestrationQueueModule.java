package io.harness;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.harness.config.PublisherConfiguration;
import io.harness.delay.DelayEvent;
import io.harness.delay.DelayEventListener;
import io.harness.engine.events.OrchestrationEventListener;
import io.harness.mongo.queue.QueueFactory;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.interrupts.InterruptEvent;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.execution.NodeExecutionEventListener;
import io.harness.pms.sdk.core.interrupt.InterruptEventListener;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.queue.QueuePublisher;
import io.harness.version.VersionInfoManager;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import org.springframework.data.mongodb.core.MongoTemplate;

public class OrchestrationQueueModule extends AbstractModule {
  private static OrchestrationQueueModule instance;
  private final OrchestrationModuleConfig config;

  public static synchronized OrchestrationQueueModule getInstance(OrchestrationModuleConfig config) {
    if (instance == null) {
      instance = new OrchestrationQueueModule(config);
    }
    return instance;
  }

  public OrchestrationQueueModule(OrchestrationModuleConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueueListener<DelayEvent>>() {}).to(DelayEventListener.class);
    bind(new TypeLiteral<QueueListener<NodeExecutionEvent>>() {}).to(NodeExecutionEventListener.class);
    bind(new TypeLiteral<QueueListener<InterruptEvent>>() {}).to(InterruptEventListener.class);

    if (!config.isWithPMS()) {
      bind(new TypeLiteral<QueueListener<OrchestrationEvent>>() {}).to(OrchestrationEventListener.class);
    }
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
      Injector injector, PublisherConfiguration config, MongoTemplate mongoTemplate) {
    return QueueFactory.createNgQueuePublisher(injector, OrchestrationEvent.class, emptyList(), config, mongoTemplate);
  }

  @Provides
  @Singleton
  QueuePublisher<NodeExecutionEvent> executionEventQueuePublisher(
      Injector injector, PublisherConfiguration config, MongoTemplate mongoTemplate) {
    return QueueFactory.createNgQueuePublisher(injector, NodeExecutionEvent.class, emptyList(), config, mongoTemplate);
  }

  @Provides
  @Singleton
  QueuePublisher<InterruptEvent> interruptEventQueuePublisher(
      Injector injector, PublisherConfiguration config, MongoTemplate mongoTemplate) {
    return QueueFactory.createNgQueuePublisher(injector, InterruptEvent.class, emptyList(), config, mongoTemplate);
  }
}
