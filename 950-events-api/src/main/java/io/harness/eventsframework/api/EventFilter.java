package io.harness.eventsframework.api;

import io.harness.eventsframework.consumer.Message;

public interface EventFilter {
  boolean filter(Message message);
}
