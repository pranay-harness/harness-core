package io.harness;

import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_FACILITATOR_EVENT_BATCH_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_FACILITATOR_EVENT_TOPIC;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_INTERRUPT_BATCH_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_INTERRUPT_TOPIC;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_NODE_START_EVENT_BATCH_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_NODE_START_EVENT_TOPIC;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_ORCHESTRATION_EVENT_BATCH_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_ORCHESTRATION_EVENT_TOPIC;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_FACILITATOR_CONSUMER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_FACILITATOR_LISTENER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_INTERRUPT_CONSUMER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_INTERRUPT_LISTENER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_NODE_START_CONSUMER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_NODE_START_LISTENER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_ORCHESTRATION_EVENT_CONSUMER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_ORCHESTRATION_EVENT_LISTENER;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.listener.facilitators.FacilitatorEventMessageListener;
import io.harness.pms.listener.interrupts.InterruptEventMessageListener;
import io.harness.pms.listener.node.start.NodeStartEventMessageListener;
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
          .annotatedWith(Names.named(PT_INTERRUPT_CONSUMER))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(PT_ORCHESTRATION_EVENT_CONSUMER))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));

      // facilitator
      bind(Consumer.class)
          .annotatedWith(Names.named(PT_FACILITATOR_CONSUMER))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));

      bind(Consumer.class)
          .annotatedWith(Names.named(PT_NODE_START_CONSUMER))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));

    } else {
      bind(Consumer.class)
          .annotatedWith(Names.named(PT_INTERRUPT_CONSUMER))
          .toInstance(RedisConsumer.of(PIPELINE_INTERRUPT_TOPIC, serviceName, redisConfig, Duration.ofSeconds(10),
              PIPELINE_INTERRUPT_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(PT_ORCHESTRATION_EVENT_CONSUMER))
          .toInstance(RedisConsumer.of(PIPELINE_ORCHESTRATION_EVENT_TOPIC, serviceName, redisConfig,
              Duration.ofSeconds(10), PIPELINE_ORCHESTRATION_EVENT_BATCH_SIZE));

      // facilitator
      bind(Consumer.class)
          .annotatedWith(Names.named(PT_FACILITATOR_CONSUMER))
          .toInstance(RedisConsumer.of(PIPELINE_FACILITATOR_EVENT_TOPIC, serviceName, redisConfig,
              Duration.ofSeconds(10), PIPELINE_FACILITATOR_EVENT_BATCH_SIZE));

      bind(Consumer.class)
          .annotatedWith(Names.named(PT_NODE_START_CONSUMER))
          .toInstance(RedisConsumer.of(PIPELINE_NODE_START_EVENT_TOPIC, serviceName, redisConfig,
              Duration.ofSeconds(10), PIPELINE_NODE_START_EVENT_BATCH_SIZE));
    }
    bind(MessageListener.class)
        .annotatedWith(Names.named(PT_INTERRUPT_LISTENER))
        .to(InterruptEventMessageListener.class);

    bind(MessageListener.class)
        .annotatedWith(Names.named(PT_ORCHESTRATION_EVENT_LISTENER))
        .to(OrchestrationEventMessageListener.class);

    // facilitator listener
    bind(MessageListener.class)
        .annotatedWith(Names.named(PT_FACILITATOR_LISTENER))
        .to(FacilitatorEventMessageListener.class);

    bind(MessageListener.class)
        .annotatedWith(Names.named(PT_NODE_START_LISTENER))
        .to(NodeStartEventMessageListener.class);
  }
}
