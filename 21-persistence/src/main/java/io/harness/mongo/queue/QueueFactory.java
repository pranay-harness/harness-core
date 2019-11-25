package io.harness.mongo.queue;

import com.google.inject.Injector;

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
      Injector injector, Class<T> klass, VersionType versionType, PublisherConfiguration configuration) {
    if (configuration.isPublisherActive(klass)) {
      final MongoQueuePublisher mongoQueuePublisher = new MongoQueuePublisher(klass.getSimpleName(), versionType);
      injector.injectMembers(mongoQueuePublisher);
      return mongoQueuePublisher;
    } else {
      logger.error("NoOpQueue has been setup for eventType:[{}]", klass.getName());
      return new NoopQueuePublisher();
    }
  }

  public static <T extends Queuable> QueueConsumer<T> createQueueConsumer(Injector injector, Class<T> klass,
      VersionType versionType, Duration heartbeat, PublisherConfiguration configuration) {
    if (configuration.isPublisherActive(klass)) {
      final MongoQueueConsumer mongoQueueConsumer = new MongoQueueConsumer(klass, versionType, heartbeat);
      injector.injectMembers(mongoQueueConsumer);
      return mongoQueueConsumer;
    } else {
      logger.error("NoOpQueue has been setup for eventType:[{}]", klass.getName());
      return new NoopQueueConsumer<>();
    }
  }
}
