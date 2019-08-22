package io.harness.batch.processing.reader;

import io.harness.event.grpc.PublishedMessage;
import org.springframework.batch.item.ItemReader;

public interface EventReader {
  int READER_BATCH_SIZE = 10;

  ItemReader<PublishedMessage> getEventReader(String messageType, Long startDate, Long endDate);
}
