package io.harness.eventsframework.api;

import io.harness.eventsframework.ConsumerShutdownException;
import io.harness.eventsframework.consumer.Message;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface Consumer {
  List<Message> read(long maxWaitTime, TimeUnit unit) throws ConsumerShutdownException;
  void acknowledge(String messageId) throws ConsumerShutdownException;
  void shutdown();
}
