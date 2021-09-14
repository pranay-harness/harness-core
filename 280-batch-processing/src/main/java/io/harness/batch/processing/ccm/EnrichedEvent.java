/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
