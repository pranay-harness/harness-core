package io.harness.pms.sdk.execution.events.interrupts;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;
import static io.harness.pms.sdk.execution.events.PmsSdkEventFrameworkConstants.SDK_PROCESSOR_SERVICE;

import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.interrupt.InterruptEventHandler;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterruptEventMessageListener extends PmsAbstractMessageListener<InterruptEvent, InterruptEventHandler> {
  @Inject
  public InterruptEventMessageListener(@Named(SDK_SERVICE_NAME) String serviceName,
      InterruptEventHandler interruptEventHandler, @Named(SDK_PROCESSOR_SERVICE) ExecutorService executorService) {
    super(serviceName, InterruptEvent.class, interruptEventHandler, executorService);
  }
}
