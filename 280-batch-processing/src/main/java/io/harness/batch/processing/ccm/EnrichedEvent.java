package io.harness.batch.processing.ccm;

import lombok.Value;
import software.wings.beans.instance.HarnessServiceInfo;

import javax.annotation.Nullable;

@Value
public class EnrichedEvent<T> {
  String accountId;
  long occurredAt;

  T event;
  @Nullable HarnessServiceInfo harnessServiceInfo;
}
