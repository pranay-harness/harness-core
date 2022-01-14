package io.harness.aggregator;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StreamingMetrics {
  long numberOfDisconnects;
  long numberOfPrimaryElections;
  long millisBehindSource;
  boolean connected;
  long millisSinceLastEvent;
  int queueTotalCapacity;
  int queueRemainingCapacity;
  String lastEvent;
  long totalNumberOfEventsSeen;
  long currentQueueSizeInBytes;
}
