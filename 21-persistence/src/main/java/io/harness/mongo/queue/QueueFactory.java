package io.harness.mongo.queue;

import io.harness.config.PublisherConfiguration;
import io.harness.mongo.NoopQueue;
import io.harness.queue.Queue;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@UtilityClass
@Slf4j
public class QueueFactory {
  public static <T> Queue<T> createQueue(Class<T> klass, PublisherConfiguration configuration) {
    if (configuration.isPublisherActive(klass)) {
      return new MongoQueue(klass);
    } else {
      logger.error("NoOpQueue has been setup for eventType:[{}]", klass.getName());
      return new NoopQueue();
    }
  }

  public static <T> Queue<T> createQueue(
      Class<T> klass, Duration heartbeat, boolean filterWithVersion, PublisherConfiguration configuration) {
    if (configuration.isPublisherActive(klass)) {
      return new MongoQueue(klass, heartbeat, filterWithVersion);
    } else {
      logger.error("NoOpQueue has been setup for eventType:[{}]", klass.getName());
      return new NoopQueue();
    }
  }

  public static <T> Queue createQueue(Class<T> klass, Duration heartbeat, PublisherConfiguration configuration) {
    if (configuration.isPublisherActive(klass)) {
      return new MongoQueue(klass, heartbeat);
    } else {
      logger.error("NoOpQueue has been setup for eventType:[{}]", klass.getName());
      return new NoopQueue();
    }
  }
}
