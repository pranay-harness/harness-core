/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delay;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class DelayEventListener extends QueueListener<DelayEvent> {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Inject
  public DelayEventListener(QueueConsumer<DelayEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(DelayEvent message) {
    log.info("Notifying for DelayEvent with resumeId {}", message.getResumeId());

    waitNotifyEngine.doneWith(
        message.getResumeId(), DelayEventNotifyData.builder().context(message.getContext()).build());
  }
}
