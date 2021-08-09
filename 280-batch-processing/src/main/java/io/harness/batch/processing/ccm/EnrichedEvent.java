package io.harness.batch.processing.ccm;

import io.harness.ccm.commons.beans.HarnessServiceInfo;

import javax.annotation.Nullable;
import lombok.Value;

@Value
public class EnrichedEvent<T> {
  String accountId;
  long occurredAt;

  T event;
  @Nullable HarnessServiceInfo harnessServiceInfo;
}
