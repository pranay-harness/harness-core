package io.harness.batch.processing.ccm;

import io.harness.ccm.commons.beans.HarnessServiceInfo;
import lombok.Value;

import javax.annotation.Nullable;

@Value
public class EnrichedEvent<T> {
  String accountId;
  long occurredAt;

  T event;
  @Nullable HarnessServiceInfo harnessServiceInfo;
}
