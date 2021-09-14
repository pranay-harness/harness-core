/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sdk.execution.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.waiter.NotifyEventListenerHelper;
import io.harness.waiter.notify.NotifyEventProto;

import com.google.inject.Inject;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class NotifyEventHandler implements PmsCommonsBaseEventHandler<NotifyEventProto> {
  @Inject NotifyEventListenerHelper notifyEventListenerHelper;

  @Override
  public void handleEvent(NotifyEventProto event, Map<String, String> metadataMap, long timestamp) {
    notifyEventListenerHelper.onMessage(event.getWaitInstanceId());
  }
}
