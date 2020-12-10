package io.harness.ng.eventsframework;

import io.harness.EventsFrameworkConfiguration;
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.impl.NoOpProducer;
import io.harness.eventsframework.impl.RedisProducer;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EventsFrameworkModule extends AbstractModule {
  public static final String PROJECT_UPDATE_PRODUCER = "project_update_producer";
  public static final String CONNECTOR_UPDATE_PRODUCER = "connector_update_producer";

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
    } else {
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(PROJECT_UPDATE_PRODUCER))
          .toInstance(new RedisProducer("project_update", redisConfig));
      bind(AbstractProducer.class)
          .annotatedWith(Names.named(CONNECTOR_UPDATE_PRODUCER))
          .toInstance(new RedisProducer("connector_update", redisConfig));
    }
  }
}