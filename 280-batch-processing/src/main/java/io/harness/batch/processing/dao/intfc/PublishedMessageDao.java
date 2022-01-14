package io.harness.batch.processing.dao.intfc;

import io.harness.ccm.commons.entities.events.PublishedMessage;

import java.util.List;

public interface PublishedMessageDao {
  List<PublishedMessage> fetchPublishedMessage(
      String accountId, String messageType, Long startTime, Long endTime, int batchSize);
}
