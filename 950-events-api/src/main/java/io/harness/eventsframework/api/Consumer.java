package io.harness.eventsframework.api;

import io.harness.eventsframework.consumer.Message;

import java.time.Duration;
import java.util.List;

public interface Consumer {
  List<Message> read(Duration maxWaitTime) throws ConsumerShutdownException;
  void acknowledge(String messageId) throws ConsumerShutdownException;
  void shutdown();
}
