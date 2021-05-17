package io.harness.eventsframework.api;

import io.harness.eventsframework.producer.Message;

public interface Producer {
  String send(Message message);
  void shutdown();
}
