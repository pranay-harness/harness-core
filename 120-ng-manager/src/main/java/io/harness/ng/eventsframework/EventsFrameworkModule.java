package io.harness.ng.eventsframework;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EventsFrameworkModule extends AbstractModule {
  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;

  @Override
  protected void configure() {
    RedisConfig redisConfig = this.eventsFrameworkConfiguration.getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(AbstractConsumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(AbstractConsumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.FEATURE_FLAG_STREAM))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
    } else {
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(RedisProducer.of(
              EventsFrameworkConstants.ENTITY_CRUD, redisConfig, EventsFrameworkConstants.ENTITY_CRUD_MAX_TOPIC_SIZE));
      bind(AbstractConsumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.ENTITY_CRUD, NG_MANAGER.getServiceId(), redisConfig,
              EventsFrameworkConstants.ENTITY_CRUD_MAX_PROCESSING_TIME));
      bind(AbstractConsumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.FEATURE_FLAG_STREAM))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.FEATURE_FLAG_STREAM, NG_MANAGER.getServiceId(),
              redisConfig, EventsFrameworkConstants.FEATURE_FLAG_MAX_PROCESSING_TIME));
    }
  }
}