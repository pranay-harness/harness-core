package io.harness.ng.core.event;

import io.harness.eventsframework.consumer.Message;

public interface MessageProcessor {
  void processMessage(Message message);
}
