package io.harness.mongo.queue;

import io.harness.logging.AutoLogRemoveContext;
import io.harness.mongo.MessageLogContext;
import io.harness.queue.Queuable;
import io.harness.queue.QueuePublisher;
import io.harness.queue.TopicUtils;
import lombok.Getter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Objects;

import static io.harness.manage.GlobalContextManager.obtainGlobalContext;

public class NGMongoQueuePublisher<T extends Queuable> implements QueuePublisher<T> {
  @Getter private final String name;
  @Getter private final String topicPrefix;
  private final MongoTemplate persistence;

  public NGMongoQueuePublisher(String name, List<String> topicPrefixElements, MongoTemplate mongoTemplate) {
    this.name = name;
    this.topicPrefix = TopicUtils.combineElements(topicPrefixElements);
    this.persistence = mongoTemplate;
  }

  @Override
  public void send(T payload) {
    Objects.requireNonNull(payload);
    payload.setTopic(topicPrefix);
    store(payload);
  }

  @Override
  public void send(List<String> additionalTopicElements, T payload) {
    Objects.requireNonNull(payload);
    payload.setTopic(TopicUtils.appendElements(topicPrefix, additionalTopicElements));
    store(payload);
  }

  private void store(T payload) {
    try (AutoLogRemoveContext ignore = new AutoLogRemoveContext(
             MessageLogContext.MESSAGE_CLASS, MessageLogContext.MESSAGE_ID, MessageLogContext.MESSAGE_TOPIC)) {
      payload.setGlobalContext(obtainGlobalContext());
    }
    try {
      persistence.insert(payload);
    } catch (DuplicateKeyException duplicateKeyException) {
      // ignore
    }
  }
}
