package io.harness.event.service.intfc;

import io.harness.event.grpc.PublishedMessage;

import java.util.List;

public interface LastReceivedPublishedMessageRepository {
  void updateLastReceivedPublishedMessages(List<PublishedMessage> publishedMessages);
}
