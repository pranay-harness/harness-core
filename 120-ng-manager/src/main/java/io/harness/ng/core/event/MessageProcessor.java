package io.harness.ng.core.event;

import io.harness.eventsframework.consumer.Message;

// Don't use this interface, instead use MessageListener
public interface MessageProcessor {
  boolean processMessage(Message message);
}
