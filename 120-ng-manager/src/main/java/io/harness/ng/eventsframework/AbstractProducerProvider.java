package io.harness.ng.eventsframework;

import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.impl.NoOpProducer;
import io.harness.eventsframework.impl.RedisProducer;
import io.harness.redis.RedisConfig;

import com.google.inject.Provider;

public class AbstractProducerProvider implements Provider<AbstractProducer> {
  String topicName;
  RedisConfig redisConfig;

  public AbstractProducerProvider(String topicName, RedisConfig redisConfig) {
    this.topicName = topicName;
    this.redisConfig = redisConfig;
  }

  @Override
  public AbstractProducer get() {
    if (this.redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      return new NoOpProducer("dummy_topic_name");
    } else {
      return new RedisProducer(topicName, redisConfig);
    }
  }
}
