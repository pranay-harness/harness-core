/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.execution.consumers;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.execution.utils.SdkResponseHandler;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.events.base.PmsAbstractMessageListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class SdkResponseEventMessageListener
    extends PmsAbstractMessageListener<SdkResponseEventProto, SdkResponseHandler> {
  @Inject
  public SdkResponseEventMessageListener(@Named(SDK_SERVICE_NAME) String serviceName,
      SdkResponseHandler sdkResponseHandler, @Named("EngineExecutorService") ExecutorService executorService) {
    super(serviceName, SdkResponseEventProto.class, sdkResponseHandler, executorService);
  }

  @Override
  public boolean isProcessable(Message message) {
    return true;
  }
}
