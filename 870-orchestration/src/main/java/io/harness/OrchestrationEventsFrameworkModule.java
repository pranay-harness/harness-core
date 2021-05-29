package io.harness;

import static io.harness.AuthorizationServiceHeader.PIPELINE_SERVICE;
import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_PRODUCER;
import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_TOPIC;
import static io.harness.pms.events.PmsEventFrameworkConstants.MAX_TOPIC_SIZE;
import static io.harness.pms.events.PmsEventFrameworkConstants.ORCHESTRATION_EVENT_PRODUCER;
import static io.harness.pms.events.PmsEventFrameworkConstants.ORCHESTRATION_EVENT_TOPIC;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class OrchestrationEventsFrameworkModule extends AbstractModule {
  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;

  public OrchestrationEventsFrameworkModule(EventsFrameworkConfiguration eventsFrameworkConfiguration) {
    this.eventsFrameworkConfiguration = eventsFrameworkConfiguration;
  }

  @Override
  protected void configure() {
    RedisConfig redisConfig = this.eventsFrameworkConfiguration.getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      bind(Producer.class)
          .annotatedWith(Names.named(INTERRUPT_PRODUCER))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(ORCHESTRATION_EVENT_PRODUCER))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
    } else {
      bind(Producer.class)
          .annotatedWith(Names.named(INTERRUPT_PRODUCER))
          .toInstance(RedisProducer.of(INTERRUPT_TOPIC, redisConfig, MAX_TOPIC_SIZE, PIPELINE_SERVICE.getServiceId()));

      bind(Producer.class)
          .annotatedWith(Names.named(ORCHESTRATION_EVENT_PRODUCER))
          .toInstance(RedisProducer.of(
              ORCHESTRATION_EVENT_TOPIC, redisConfig, MAX_TOPIC_SIZE, PIPELINE_SERVICE.getServiceId()));
    }
  }
}
