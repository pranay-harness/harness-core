package io.harness.pms.listener.orchestrationevent;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.events.base.PmsAbstractBaseMessageListenerWithObservers;
import io.harness.pms.execution.utils.OrchestrationEventUtils;
import io.harness.pms.sdk.core.execution.events.orchestration.SdkOrchestrationEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationEventMessageListener extends PmsAbstractBaseMessageListenerWithObservers<OrchestrationEvent> {
  private final SdkOrchestrationEventHandler sdkOrchestrationEventHandler;

  @Inject
  public OrchestrationEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, SdkOrchestrationEventHandler sdkOrchestrationEventHandler) {
    super(serviceName, OrchestrationEvent.class);
    this.sdkOrchestrationEventHandler = sdkOrchestrationEventHandler;
  }

  // Todo: Make changes here
  public boolean processMessageInternal(OrchestrationEvent event) {
    try (AutoLogContext ignore = OrchestrationEventUtils.obtainLogContext(event)) {
      log.info("Orchestration Event Processing Starting for event type {}", event.getEventType());
      return sdkOrchestrationEventHandler.handleEvent(event);
    } catch (Exception ex) {
      log.error("Orchestration Event Processing failed for event type {}", event.getEventType(), ex);
      return true;
    }
  }
}
