package io.harness.event.client;

import io.harness.event.PublishMessage;

public interface EventPublisher {
  void publish(PublishMessage publishMessage);

  void shutdown();
}
