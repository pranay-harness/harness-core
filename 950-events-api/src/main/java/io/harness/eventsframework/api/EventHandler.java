package io.harness.eventsframework.api;

import io.harness.eventsframework.consumer.Message;

public interface EventHandler {
  boolean handle(Message message);
}
