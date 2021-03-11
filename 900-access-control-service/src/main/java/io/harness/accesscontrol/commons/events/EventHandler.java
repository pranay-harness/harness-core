package io.harness.accesscontrol.commons.events;

import io.harness.eventsframework.consumer.Message;

public interface EventHandler {
  boolean handle(Message message);
}
