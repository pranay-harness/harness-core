package io.harness.mongo.queue;

import static io.harness.manage.GlobalContextManager.obtainGlobalContext;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import io.harness.queue.Queuable;
import io.harness.queue.QueuePublisher;
import io.harness.queue.TopicUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Slf4j
public class MongoQueuePublisher<T extends Queuable> implements QueuePublisher<T> {
  @Getter private final String name;
  @Getter private final String topicPrefix;

  @Inject private HPersistence persistence;

  public MongoQueuePublisher(String name, List<String> topicPrefixElements) {
    this.name = name;
    topicPrefix = TopicUtils.combineElements(topicPrefixElements);
  }

  @Override
  public void send(final T payload) {
    Objects.requireNonNull(payload);
    payload.setGlobalContext(obtainGlobalContext());
    payload.setTopic(topicPrefix);
    persistence.insertIgnoringDuplicateKeys(payload);
  }
}
