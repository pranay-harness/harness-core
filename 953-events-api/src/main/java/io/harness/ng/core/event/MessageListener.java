package io.harness.ng.core.event;

import io.harness.eventsframework.consumer.Message;

public interface MessageListener {
  boolean handleMessage(Message message);
}
