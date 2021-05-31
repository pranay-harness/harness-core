package io.harness.pms.sdk.core;

import static io.harness.pms.events.PmsEventFrameworkConstants.SDK_RESPONSE_EVENT_TOPIC;
import static io.harness.pms.sdk.core.PmsSdkCoreEventsFrameworkConstants.SDK_RESPONSE_EVENT_PRODUCER;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.pms.events.PmsEventFrameworkConstants;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

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
      bind(Producer.class)
          .annotatedWith(Names.named(SDK_RESPONSE_EVENT_PRODUCER))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
    } else {
      bind(Producer.class)
          .annotatedWith(Names.named(SDK_RESPONSE_EVENT_PRODUCER))
          .toInstance(RedisProducer.of(
              SDK_RESPONSE_EVENT_TOPIC, redisConfig, PmsEventFrameworkConstants.MAX_TOPIC_SIZE, serviceName));
    }
  }
}
