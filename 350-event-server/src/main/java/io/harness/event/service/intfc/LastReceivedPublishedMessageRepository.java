package io.harness.event.service.intfc;

import io.harness.ccm.commons.entities.events.PublishedMessage;

import java.util.List;

public interface LastReceivedPublishedMessageRepository {
  void updateLastReceivedPublishedMessages(List<PublishedMessage> publishedMessages);
}
