/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.events.base;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OwnedBy(HarnessTeam.PIPELINE)
public class NoopPmsMessageListener extends PmsAbstractMessageListener<InterruptEvent, NoopPmsEventHandler> {
  public NoopPmsMessageListener(String serviceName, NoopPmsEventHandler handler) {
    super(serviceName, InterruptEvent.class, handler, Executors.newSingleThreadExecutor());
  }

  public NoopPmsMessageListener(String serviceName, NoopPmsEventHandler handler, ExecutorService executorService) {
    super(serviceName, InterruptEvent.class, handler, executorService);
  }
}
