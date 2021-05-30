package io.harness;

import static io.harness.pms.listener.PmsUtilityConsumerConstants.INTERRUPT_BATCH_SIZE;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.INTERRUPT_CONSUMER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.INTERRUPT_LISTENER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.INTERRUPT_TOPIC;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.ORCHESTRATION_EVENT_BATCH_SIZE;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.ORCHESTRATION_EVENT_CONSUMER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.ORCHESTRATION_EVENT_LISTENER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.ORCHESTRATION_EVENT_TOPIC;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.listener.interrupts.InterruptEventMessageListener;
import io.harness.pms.listener.orchestrationevent.OrchestrationEventMessageListener;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import java.time.Duration;

public class PipelineServiceUtilityModule extends AbstractModule {
  private static PipelineServiceUtilityModule instance;

  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;
  private final String serviceName;

  public static PipelineServiceUtilityModule getInstance(EventsFrameworkConfiguration config, String serviceName) {
    if (instance == null) {
      instance = new PipelineServiceUtilityModule(config, serviceName);
    }
    return instance;
  }

  private PipelineServiceUtilityModule(EventsFrameworkConfiguration config, String serviceName) {
    this.eventsFrameworkConfiguration = config;
    this.serviceName = serviceName;
  }

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
    } else {
      bind(Consumer.class)
          .annotatedWith(Names.named(INTERRUPT_CONSUMER))
          .toInstance(RedisConsumer.of(
              INTERRUPT_TOPIC, serviceName, redisConfig, Duration.ofSeconds(10), INTERRUPT_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(ORCHESTRATION_EVENT_CONSUMER))
          .toInstance(RedisConsumer.of(ORCHESTRATION_EVENT_TOPIC, serviceName, redisConfig, Duration.ofSeconds(10),
              ORCHESTRATION_EVENT_BATCH_SIZE));
    }
    bind(MessageListener.class).annotatedWith(Names.named(INTERRUPT_LISTENER)).to(InterruptEventMessageListener.class);
    bind(MessageListener.class)
        .annotatedWith(Names.named(ORCHESTRATION_EVENT_LISTENER))
        .to(OrchestrationEventMessageListener.class);
  }
}
