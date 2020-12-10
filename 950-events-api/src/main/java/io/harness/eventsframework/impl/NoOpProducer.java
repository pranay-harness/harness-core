package io.harness.eventsframework.impl;

import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.producer.Message;

public class NoOpProducer extends AbstractProducer {
  public NoOpProducer(String topicName) {
    super(topicName);
  }

  @Override
  public String send(Message message) {
    return "dummy-message-id";
  }
}
