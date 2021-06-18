package io.harness.pms.sdk.execution.events.interrupts;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.interrupt.InterruptEventHandler;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterruptEventMessageListener extends PmsAbstractMessageListener<InterruptEvent> {
  private final InterruptEventHandler interruptEventHandler;

  @Inject
  public InterruptEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, InterruptEventHandler interruptEventHandler) {
    super(serviceName, InterruptEvent.class);
    this.interruptEventHandler = interruptEventHandler;
  }

  public void processMessage(InterruptEvent event, Map<String, String> metadataMap, Long timestamp) {
    interruptEventHandler.handleEvent(event);
  }
}
