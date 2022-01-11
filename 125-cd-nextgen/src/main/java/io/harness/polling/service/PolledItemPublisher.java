/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
