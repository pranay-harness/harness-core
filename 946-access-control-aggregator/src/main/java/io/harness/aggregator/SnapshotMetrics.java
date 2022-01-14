package io.harness.aggregator;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SnapshotMetrics {
  long millisSinceLastEvent;
  int queueTotalCapacity;
  int queueRemainingCapacity;
  String lastEvent;
  long totalNumberOfEventsSeen;
  long currentQueueSizeInBytes;
  boolean snapshotRunning;
  boolean snapshotCompleted;
  long snapshotDurationInSeconds;
}
