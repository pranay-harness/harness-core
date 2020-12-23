package io.harness.eventsframework.impl.noop;

import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.api.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NoOpConsumer extends AbstractConsumer {
  public NoOpConsumer(String topicName, String groupName) {
    super(topicName, groupName);
  }

  @Override
  public List<Message> read(long maxWaitTime, TimeUnit unit) throws ConsumerShutdownException {
    return Collections.emptyList();
  }

  @Override
  public void acknowledge(String messageId) throws ConsumerShutdownException {}

  @Override
  public void shutdown() {}

  public static NoOpConsumer of(String topicName, String groupName) {
    return new NoOpConsumer(topicName, groupName);
  }
}
