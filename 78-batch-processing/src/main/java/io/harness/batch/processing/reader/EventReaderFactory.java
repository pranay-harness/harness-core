package io.harness.batch.processing.reader;

import io.harness.event.grpc.PublishedMessage;
import org.springframework.batch.item.ItemReader;
import software.wings.beans.SettingAttribute;

public interface EventReaderFactory {
  ItemReader<PublishedMessage> getEventReader(String accountId, String messageType, Long startDate, Long endDate);
  ItemReader<SettingAttribute> getS3JobConfigReader(String accountId);
  ItemReader<PublishedMessage> getEventReader(
      String accountId, String messageType, Long startDate, Long endDate, int batchSize);
}
