/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import static io.harness.pms.sdk.PmsSdkModuleUtils.CORE_EXECUTOR_NAME;
import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.eventsframework.consumer.Message;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.execution.events.NotifyEventHandler;
import io.harness.waiter.notify.NotifyEventProto;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;

public class PmsNotifyEventMessageListener extends PmsAbstractMessageListener<NotifyEventProto, NotifyEventHandler> {
  @Inject
  public PmsNotifyEventMessageListener(@Named(SDK_SERVICE_NAME) String serviceName,
      NotifyEventHandler createPartialPlanEventHandler, @Named(CORE_EXECUTOR_NAME) ExecutorService executorService) {
    super(serviceName, NotifyEventProto.class, createPartialPlanEventHandler, executorService);
  }

  @Override
  public boolean isProcessable(Message message) {
    return true;
  }
}
