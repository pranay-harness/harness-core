/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.plan.consumers;

import static io.harness.pms.sdk.PmsSdkModuleUtils.CORE_EXECUTOR_NAME;
import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.eventsframework.consumer.Message;
import io.harness.pms.contracts.plan.PartialPlanResponse;
import io.harness.pms.events.base.PmsAbstractMessageListener;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;

public class PartialPlanResponseEventMessageListener
    extends PmsAbstractMessageListener<PartialPlanResponse, PartialPlanResponseEventHandler> {
  @Inject
  public PartialPlanResponseEventMessageListener(@Named(SDK_SERVICE_NAME) String serviceName,
      PartialPlanResponseEventHandler partialPlanResponseEventHandler,
      @Named(CORE_EXECUTOR_NAME) ExecutorService executorService) {
    super(serviceName, PartialPlanResponse.class, partialPlanResponseEventHandler, executorService);
  }

  @Override
  public boolean isProcessable(Message message) {
    return true;
  }
}
