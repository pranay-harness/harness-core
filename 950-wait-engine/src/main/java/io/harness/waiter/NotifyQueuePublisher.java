package io.harness.waiter;

public interface NotifyQueuePublisher {
  void send(NotifyEvent payload);
}
