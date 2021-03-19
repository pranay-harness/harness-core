package io.harness.outbox;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboxEventIteratorConfiguration {
  int threadPoolSize;
  long intervalInSeconds;
  long targetIntervalInSeconds;
  long acceptableNoAlertDelayInSeconds;
  long maximumOutboxEventHandlingAttempts;
}
