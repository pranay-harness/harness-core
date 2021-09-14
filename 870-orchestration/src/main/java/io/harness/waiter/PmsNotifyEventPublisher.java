/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsNotifyEventPublisher extends RedisNotifyQueuePublisher {
  @Inject
  public PmsNotifyEventPublisher(@Named(EventsFrameworkConstants.PMS_ORCHESTRATION_NOTIFY_EVENT) Producer producer) {
    super(producer);
  }
}
