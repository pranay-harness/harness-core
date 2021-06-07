package io.harness.pms.listener.orchestrationevent;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.events.base.PmsAbstractBaseMessageListenerWithObservers;
import io.harness.pms.execution.utils.OrchestrationEventUtils;
import io.harness.pms.sdk.core.execution.events.orchestration.SdkOrchestrationEventListenerHelper;
import io.harness.serializer.ProtoUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class OrchestrationEventMessageListener extends PmsAbstractBaseMessageListenerWithObservers<OrchestrationEvent> {
  private final SdkOrchestrationEventListenerHelper helper;

  @Inject
  public OrchestrationEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, SdkOrchestrationEventListenerHelper helper) {
    super(serviceName, OrchestrationEvent.class);
    this.helper = helper;
  }

  // Todo: Make changes here
  public boolean processMessageInternal(OrchestrationEvent event) {
    try (AutoLogContext ignore = OrchestrationEventUtils.obtainLogContext(event)) {
      log.info("Orchestration Event Processing Starting for event type {}", event.getEventType());
      helper.handleEvent(io.harness.pms.sdk.core.events.OrchestrationEvent.builder()
                             .eventType(event.getEventType())
                             .ambiance(event.getAmbiance())
                             .createdAt(ProtoUtils.timestampToUnixMillis(event.getCreatedAt()))
                             .status(event.getStatus())
                             .resolvedStepParameters(event.getStepParameters().toStringUtf8())
                             .serviceName(event.getServiceName())
                             .build());
      return true;
    } catch (Exception ex) {
      log.error("Orchestration Event Processing failed for event type {}", event.getEventType(), ex);
      return true;
    }
  }
}
