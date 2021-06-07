package io.harness.event.grpc;

import io.harness.ccm.commons.entities.events.PublishedMessage;

public interface MessageProcessor {
  void process(PublishedMessage publishedMessage);
}
