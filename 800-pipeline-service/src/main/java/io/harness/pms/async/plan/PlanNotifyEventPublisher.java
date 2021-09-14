/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.async.plan;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.waiter.RedisNotifyQueuePublisher;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.PIPELINE)
public class PlanNotifyEventPublisher extends RedisNotifyQueuePublisher {
  @Inject
  PlanNotifyEventPublisher(@Named(EventsFrameworkConstants.PLAN_NOTIFY_EVENT_PRODUCER) Producer producer) {
    super(producer);
  }
}
