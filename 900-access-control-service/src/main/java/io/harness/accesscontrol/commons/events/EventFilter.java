package io.harness.accesscontrol.commons.events;

import io.harness.eventsframework.consumer.Message;

public interface EventFilter {
  boolean filter(Message message);
}
