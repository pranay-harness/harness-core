package io.harness.event.publisher;

import io.harness.event.model.Event;

public interface EventPublisher {
  void publishEvent(Event event) throws EventPublishException;
}
