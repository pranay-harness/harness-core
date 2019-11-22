package io.harness.mongo.queue;

import io.harness.config.PublisherConfiguration;
import io.harness.queue.NoopQueueConsumer;
import io.harness.queue.NoopQueuePublisher;
import io.harness.queue.Queuable;
import io.harness.queue.Queue.VersionType;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueuePublisher;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@UtilityClass
@Slf4j
public class QueueFactory {
  public static <T extends Queuable> QueuePublisher<T> createQueuePublisher(
      Class<T> klass, VersionType versionType, PublisherConfiguration configuration) {
    if (configuration.isPublisherActive(klass)) {
      return new MongoQueuePublisher(klass.getSimpleName(), versionType);
    } else {
      logger.error("NoOpQueue has been setup for eventType:[{}]", klass.getName());
      return new NoopQueuePublisher();
    }
  }

  public static <T extends Queuable> QueueConsumer<T> createQueueConsumer(
      Class<T> klass, VersionType versionType, Duration heartbeat, PublisherConfiguration configuration) {
    if (configuration.isPublisherActive(klass)) {
      return new MongoQueueConsumer(klass, versionType, heartbeat);
    } else {
      logger.error("NoOpQueue has been setup for eventType:[{}]", klass.getName());
      return new NoopQueueConsumer<>();
    }
  }
}
