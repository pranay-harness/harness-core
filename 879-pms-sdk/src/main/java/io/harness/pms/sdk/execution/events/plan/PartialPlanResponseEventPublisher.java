/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sdk.execution.events.plan;

import static io.harness.pms.sdk.core.PmsSdkCoreEventsFrameworkConstants.PARTIAL_PLAN_RESPONSE_EVENT_PRODUCER;

import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.pms.contracts.plan.PartialPlanResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.HashMap;

public class PartialPlanResponseEventPublisher {
  @Inject @Named(PARTIAL_PLAN_RESPONSE_EVENT_PRODUCER) private Producer eventProducer;

  public void publishEvent(PartialPlanResponse event) {
    eventProducer.send(Message.newBuilder().putAllMetadata(new HashMap<>()).setData(event.toByteString()).build());
  }
}
