package io.harness.eventsframework.api;

import io.harness.eventsframework.consumer.Message;

import java.util.Optional;

public interface Consumer {
  Optional<Message> read();
  void acknowledge(String messageId);
}
