package io.harness.ng.eventsframework;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;

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
  public static final String PROJECT_UPDATE_PRODUCER = "project_update_producer";
  public static final String CONNECTOR_UPDATE_PRODUCER = "connector_update_producer";
  public static final String SETUP_USAGE_CREATE = "setup_usage_create";
  public static final String SETUP_USAGE_DELETE = "setup_usage_delete";

  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;

  @Override
  protected void configure() {
    RedisConfig redisConfig = this.eventsFrameworkConfiguration.getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(PROJECT_UPDATE_PRODUCER))
          .toInstance(new NoOpProducer("dummy_topic_name"));
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(CONNECTOR_UPDATE_PRODUCER))
          .toInstance(new NoOpProducer("dummy_topic_name"));
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(SETUP_USAGE_CREATE))
          .toInstance(new NoOpProducer("dummy_topic_name"));
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(SETUP_USAGE_DELETE))
          .toInstance(new NoOpProducer("dummy_topic_name"));
      bind(AbstractConsumer.class)
          .annotatedWith(Names.named(SETUP_USAGE_CREATE))
          .toInstance(new NoOpConsumer("dummy_topic_name", "dummy_group_name", "dummy_name"));
      bind(AbstractConsumer.class)
          .annotatedWith(Names.named(SETUP_USAGE_DELETE))
          .toInstance(new NoOpConsumer("dummy_topic_name", "dummy_group_name", "dummy_name"));
    } else {
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(PROJECT_UPDATE_PRODUCER))
          .toInstance(new RedisProducer("project_update", redisConfig));
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(CONNECTOR_UPDATE_PRODUCER))
          .toInstance(new RedisProducer("connector_update", redisConfig));
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