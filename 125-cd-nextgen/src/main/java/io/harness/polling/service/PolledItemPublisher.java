/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.polling.service;

import static io.harness.eventsframework.EventsFrameworkConstants.POLLING_EVENTS_STREAM;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.polling.contracts.PollingResponse;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class PolledItemPublisher {
  @Inject @Named(POLLING_EVENTS_STREAM) private Producer eventProducer;

  public void publishPolledItems(PollingResponse pollingResponse) {
    eventProducer.send(Message.newBuilder()
                           .putAllMetadata(ImmutableMap.of("accountId", pollingResponse.getAccountId()))
                           .setData(pollingResponse.toByteString())
                           .build());
  }
}
