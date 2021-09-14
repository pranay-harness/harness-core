/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class NotifyEventListener extends QueueListener<NotifyEvent> {
  @Inject private NotifyEventListenerHelper notifyEventListenerHelper;

  @Inject
  public NotifyEventListener(QueueConsumer<NotifyEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(NotifyEvent message) {
    notifyEventListenerHelper.onMessage(message.getWaitInstanceId());
  }
}
