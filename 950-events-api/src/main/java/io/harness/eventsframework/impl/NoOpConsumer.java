package io.harness.eventsframework.impl;

import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.consumer.Message;

import java.util.Optional;

public class NoOpConsumer extends AbstractConsumer {
  public NoOpConsumer(String topicName, String groupName, String name) {
    super(topicName, groupName, name);
  }

  @Override
  public Optional<Message> read() {
    return null;
  }

  @Override
  public void acknowledge(String messageId) {}
}
