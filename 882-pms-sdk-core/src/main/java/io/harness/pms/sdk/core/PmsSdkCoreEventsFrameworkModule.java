package io.harness.pms.sdk.core;

import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_BATCH_SIZE;
import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_CONSUMER;
import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_TOPIC;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
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
    } else {
      bind(Consumer.class)
          .annotatedWith(Names.named(INTERRUPT_CONSUMER))
          .toInstance(RedisConsumer.of(
              INTERRUPT_TOPIC, serviceName, redisConfig, Duration.ofSeconds(30), INTERRUPT_BATCH_SIZE));
    }
  }
}
