package io.harness.pms.sdk.core;

import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_BATCH_SIZE;
import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_CONSUMER;
import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_LISTENER;
import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_TOPIC;
import static io.harness.pms.events.PmsEventFrameworkConstants.ORCHESTRATION_EVENT_BATCH_SIZE;
import static io.harness.pms.events.PmsEventFrameworkConstants.ORCHESTRATION_EVENT_CONSUMER;
import static io.harness.pms.events.PmsEventFrameworkConstants.ORCHESTRATION_EVENT_LISTENER;
import static io.harness.pms.events.PmsEventFrameworkConstants.ORCHESTRATION_EVENT_TOPIC;
import static io.harness.pms.events.PmsEventFrameworkConstants.SDK_RESPONSE_EVENT_PRODUCER;
import static io.harness.pms.events.PmsEventFrameworkConstants.SDK_RESPONSE_EVENT_TOPIC;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.events.PmsEventFrameworkConstants;
import io.harness.pms.sdk.core.execution.events.orchestration.SdkOrchestrationEventEventRedisConsumerService;
import io.harness.pms.sdk.core.execution.events.orchestration.SdkOrchestrationEventMessageListener;
import io.harness.pms.sdk.core.interrupt.InterruptEventMessageListener;
import io.harness.pms.sdk.core.interrupt.InterruptRedisConsumerService;
import io.harness.pms.utils.PmsManagedService;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import java.time.Duration;

public class PmsSdkCoreEventsFrameworkModule extends AbstractModule {
  private static PmsSdkCoreEventsFrameworkModule instance;

  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;
  private final String serviceName;

  public static PmsSdkCoreEventsFrameworkModule getInstance(EventsFrameworkConfiguration config, String serviceName) {
    if (instance == null) {
      instance = new PmsSdkCoreEventsFrameworkModule(config, serviceName);
    }
    return instance;
  }

  private PmsSdkCoreEventsFrameworkModule(EventsFrameworkConfiguration config, String serviceName) {
    this.eventsFrameworkConfiguration = config;
    this.serviceName = serviceName;
  }

  @Override
  protected void configure() {
    RedisConfig redisConfig = this.eventsFrameworkConfiguration.getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      bind(Consumer.class)
          .annotatedWith(Names.named(INTERRUPT_CONSUMER))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(ORCHESTRATION_EVENT_CONSUMER))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(SDK_RESPONSE_EVENT_PRODUCER))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
    } else {
      bind(Consumer.class)
          .annotatedWith(Names.named(INTERRUPT_CONSUMER))
          .toInstance(RedisConsumer.of(
              INTERRUPT_TOPIC, serviceName, redisConfig, Duration.ofSeconds(10), INTERRUPT_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(ORCHESTRATION_EVENT_CONSUMER))
          .toInstance(RedisConsumer.of(ORCHESTRATION_EVENT_TOPIC, serviceName, redisConfig, Duration.ofSeconds(10),
              ORCHESTRATION_EVENT_BATCH_SIZE));
      bind(Producer.class)
          .annotatedWith(Names.named(SDK_RESPONSE_EVENT_PRODUCER))
          .toInstance(RedisProducer.of(
              SDK_RESPONSE_EVENT_TOPIC, redisConfig, PmsEventFrameworkConstants.MAX_TOPIC_SIZE, serviceName));
    }

    bind(MessageListener.class).annotatedWith(Names.named(INTERRUPT_LISTENER)).to(InterruptEventMessageListener.class);
    bind(MessageListener.class)
        .annotatedWith(Names.named(ORCHESTRATION_EVENT_LISTENER))
        .to(SdkOrchestrationEventMessageListener.class);

    Multibinder<PmsManagedService> serviceBinder =
        Multibinder.newSetBinder(binder(), PmsManagedService.class, Names.named("pmsManagedServices"));
    serviceBinder.addBinding().to(Key.get(InterruptRedisConsumerService.class)).in(Singleton.class);
    serviceBinder.addBinding().to(Key.get(SdkOrchestrationEventEventRedisConsumerService.class)).in(Singleton.class);
  }
}
