package io.harness.ng.core.event;

import io.harness.eventsframework.consumer.Message;

public interface ConsumerMessageProcessor {
  void processMessage(Message message);
}
