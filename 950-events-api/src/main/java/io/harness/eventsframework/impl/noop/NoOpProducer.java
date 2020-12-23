package io.harness.eventsframework.impl.noop;

import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.producer.Message;

public class NoOpProducer extends AbstractProducer {
  public NoOpProducer(String topicName) {
    super(topicName);
  }

  @Override
  public String send(Message message) throws ProducerShutdownException {
    return "dummy-message-id";
  }

  @Override
  public void shutdown() {
    // Nothing required to shutdown
  }

  public static NoOpProducer of(String topicName) {
    return new NoOpProducer(topicName);
  }
}
