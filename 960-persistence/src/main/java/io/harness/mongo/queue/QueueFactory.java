/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo.queue;

import io.harness.config.PublisherConfiguration;
import io.harness.queue.NoopQueueConsumer;
import io.harness.queue.NoopQueuePublisher;
import io.harness.queue.Queuable;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueuePublisher;

import com.google.inject.Injector;
import java.time.Duration;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@UtilityClass
@Slf4j
public class QueueFactory {
  public static <T extends Queuable> QueuePublisher<T> createQueuePublisher(
      Injector injector, Class<T> klass, List<String> topicPrefixElements, PublisherConfiguration configuration) {
    if (configuration.isPublisherActive(klass)) {
      final MongoQueuePublisher mongoQueuePublisher =
          new MongoQueuePublisher(klass.getSimpleName(), topicPrefixElements);
      injector.injectMembers(mongoQueuePublisher);
      return mongoQueuePublisher;
    } else {
      log.error("NoOpQueue has been setup for eventType:[{}]", klass.getName());
      return new NoopQueuePublisher();
    }
  }

  public static <T extends Queuable> QueueConsumer<T> createQueueConsumer(Injector injector, Class<T> klass,
      Duration heartbeat, List<List<String>> topicExpression, PublisherConfiguration configuration) {
    if (configuration.isPublisherActive(klass)) {
      final MongoQueueConsumer mongoQueueConsumer = new MongoQueueConsumer(klass, heartbeat, topicExpression);
      injector.injectMembers(mongoQueueConsumer);
      return mongoQueueConsumer;
    } else {
      log.error("NoOpQueue has been setup for eventType:[{}]", klass.getName());
      return new NoopQueueConsumer<>();
    }
  }

  public static <T extends Queuable> QueuePublisher<T> createNgQueuePublisher(Injector injector, Class<T> klass,
      List<String> topicPrefixElements, PublisherConfiguration configuration, MongoTemplate mongoTemplate) {
    if (configuration.isPublisherActive(klass)) {
      final NGMongoQueuePublisher mongoQueuePublisher =
          new NGMongoQueuePublisher(klass.getSimpleName(), topicPrefixElements, mongoTemplate);
      injector.injectMembers(mongoQueuePublisher);
      return mongoQueuePublisher;
    } else {
      log.error("NoOpQueue has been setup for eventType:[{}]", klass.getName());
      return new NoopQueuePublisher();
    }
  }

  public static <T extends Queuable> QueueConsumer<T> createNgQueueConsumer(Injector injector, Class<T> klass,
      Duration heartbeat, List<List<String>> topicExpression, PublisherConfiguration configuration,
      MongoTemplate mongoTemplate) {
    if (configuration.isPublisherActive(klass)) {
      final NGMongoQueueConsumer mongoQueueConsumer =
          new NGMongoQueueConsumer(klass, heartbeat, topicExpression, mongoTemplate);
      injector.injectMembers(mongoQueueConsumer);
      return mongoQueueConsumer;
    } else {
      log.error("NoOpQueue has been setup for eventType:[{}]", klass.getName());
      return new NoopQueueConsumer<>();
    }
  }
}
