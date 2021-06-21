package io.harness;

import static io.harness.AuthorizationServiceHeader.PIPELINE_SERVICE;
import static io.harness.OrchestrationEventsFrameworkConstants.SDK_RESPONSE_EVENT_BATCH_SIZE;
import static io.harness.OrchestrationEventsFrameworkConstants.SDK_RESPONSE_EVENT_CONSUMER;
import static io.harness.OrchestrationEventsFrameworkConstants.SDK_RESPONSE_EVENT_LISTENER;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_SDK_RESPONSE_EVENT_TOPIC;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.execution.consumers.SdkResponseEventMessageListener;
import io.harness.ng.core.event.MessageListener;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import java.time.Duration;

public class OrchestrationEventsFrameworkModule extends AbstractModule {
  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;

  public OrchestrationEventsFrameworkModule(EventsFrameworkConfiguration eventsFrameworkConfiguration) {
    this.eventsFrameworkConfiguration = eventsFrameworkConfiguration;
  }

  @Override
  protected void configure() {
    RedisConfig redisConfig = this.eventsFrameworkConfiguration.getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      bind(Consumer.class)
          .annotatedWith(Names.named(SDK_RESPONSE_EVENT_CONSUMER))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
    } else {
      bind(Consumer.class)
          .annotatedWith(Names.named(SDK_RESPONSE_EVENT_CONSUMER))
          .toInstance(RedisConsumer.of(PIPELINE_SDK_RESPONSE_EVENT_TOPIC, PIPELINE_SERVICE.getServiceId(), redisConfig,
              Duration.ofSeconds(10), SDK_RESPONSE_EVENT_BATCH_SIZE));
    }

    bind(MessageListener.class)
        .annotatedWith(Names.named(SDK_RESPONSE_EVENT_LISTENER))
        .to(SdkResponseEventMessageListener.class);
  }
}
