package io.harness.cvng.core.jobs;

import io.harness.eventsframework.consumer.Message;

public interface ConsumerMessageProcessor {
  void processMessage(Message message);
}
