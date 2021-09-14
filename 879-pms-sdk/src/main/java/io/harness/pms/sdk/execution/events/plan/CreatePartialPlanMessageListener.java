/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sdk.execution.events.plan;

import static io.harness.pms.sdk.PmsSdkModuleUtils.CORE_EXECUTOR_NAME;
import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.pms.contracts.plan.CreatePartialPlanEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;

public class CreatePartialPlanMessageListener
    extends PmsAbstractMessageListener<CreatePartialPlanEvent, CreatePartialPlanEventHandler> {
  @Inject
  public CreatePartialPlanMessageListener(@Named(SDK_SERVICE_NAME) String serviceName,
      CreatePartialPlanEventHandler createPartialPlanEventHandler,
      @Named(CORE_EXECUTOR_NAME) ExecutorService executorService) {
    super(serviceName, CreatePartialPlanEvent.class, createPartialPlanEventHandler, executorService);
  }
}
