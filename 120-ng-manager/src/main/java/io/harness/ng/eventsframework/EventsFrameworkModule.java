package io.harness.ng.eventsframework;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.EntityCRUDEventsConstants.ENTITY_CRUD;

import io.harness.EventsFrameworkConfiguration;
import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.impl.NoOpConsumer;
import io.harness.eventsframework.impl.NoOpProducer;
import io.harness.eventsframework.impl.RedisConsumer;
import io.harness.eventsframework.impl.RedisProducer;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EventsFrameworkModule extends AbstractModule {
  public static final String SETUP_USAGE_CREATE = "setup_usage_create";
  public static final String SETUP_USAGE_DELETE = "setup_usage_delete";

  private static final String DUMMY_TOPIC_NAME = "dummy_topic_name";
  private static final String DUMMY_GROUP_NAME = "dummy_group_name";
  private static final String DUMMY_NAME = "dummy_name";

  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;

  @Override
  protected void configure() {
    RedisConfig redisConfig = this.eventsFrameworkConfiguration.getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(ENTITY_CRUD))
          .toInstance(new NoOpProducer(DUMMY_TOPIC_NAME));
      bind(AbstractConsumer.class)
          .annotatedWith(Names.named(ENTITY_CRUD))
          .toInstance(new NoOpConsumer(DUMMY_TOPIC_NAME, DUMMY_GROUP_NAME, DUMMY_NAME));
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(SETUP_USAGE_CREATE))
          .toInstance(new NoOpProducer(DUMMY_TOPIC_NAME));
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(SETUP_USAGE_DELETE))
          .toInstance(new NoOpProducer(DUMMY_TOPIC_NAME));
      bind(AbstractConsumer.class)
          .annotatedWith(Names.named(SETUP_USAGE_CREATE))
          .toInstance(new NoOpConsumer(DUMMY_TOPIC_NAME, DUMMY_GROUP_NAME, DUMMY_NAME));
      bind(AbstractConsumer.class)
          .annotatedWith(Names.named(SETUP_USAGE_DELETE))
          .toInstance(new NoOpConsumer(DUMMY_TOPIC_NAME, DUMMY_GROUP_NAME, DUMMY_NAME));
    } else {
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(ENTITY_CRUD))
          .toInstance(new RedisProducer(ENTITY_CRUD, redisConfig));
      bind(AbstractConsumer.class)
          .annotatedWith(Names.named(ENTITY_CRUD))
          .toInstance(new RedisConsumer(ENTITY_CRUD, NG_MANAGER.getServiceId(), redisConfig));
      bind(AbstractConsumer.class)
          .annotatedWith(Names.named(SETUP_USAGE_CREATE))
          .toInstance(new RedisConsumer(SETUP_USAGE_CREATE, NG_MANAGER.getServiceId(), redisConfig));
      bind(AbstractConsumer.class)
          .annotatedWith(Names.named(SETUP_USAGE_DELETE))
          .toInstance(new RedisConsumer(SETUP_USAGE_DELETE, NG_MANAGER.getServiceId(), redisConfig));
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(SETUP_USAGE_CREATE))
          .toInstance(new RedisProducer(SETUP_USAGE_CREATE, redisConfig));
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(SETUP_USAGE_DELETE))
          .toInstance(new RedisProducer(SETUP_USAGE_DELETE, redisConfig));
    }
  }
}