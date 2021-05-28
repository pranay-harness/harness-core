package io.harness.pms.sdk.core.execution.events.orchestration;

import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.eventsframework.consumer.Message;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.execution.utils.OrchestrationEventUtils;
import io.harness.pms.sdk.PmsSdkModuleUtils;
import io.harness.pms.sdk.core.execution.events.base.SdkBaseEventMessageListener;
import io.harness.pms.utils.PmsConstants;
import io.harness.serializer.ProtoUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SdkOrchestrationEventMessageListener extends SdkBaseEventMessageListener<OrchestrationEvent> {
  @Inject @Named(PmsSdkModuleUtils.SDK_SERVICE_NAME) String serviceName;
  @Inject private SdkOrchestrationEventListenerHelper helper;

  public SdkOrchestrationEventMessageListener() {
    super(OrchestrationEvent.class);
  }

  public boolean processMessage(OrchestrationEvent event) {
    try (AutoLogContext ignore = OrchestrationEventUtils.obtainLogContext(event)) {
      log.error("[PMS_SDK] Orchestration Event Processing Starting for event type {}", event.getEventType());
      helper.handleEvent(io.harness.pms.sdk.core.events.OrchestrationEvent.builder()
                             .eventType(event.getEventType())
                             .ambiance(event.getAmbiance())
                             .createdAt(ProtoUtils.timestampToUnixMillis(event.getCreatedAt()))
                             .nodeExecutionProto(event.getNodeExecution())
                             .build());
      return true;
    } catch (Exception ex) {
      log.error("[PMS_SDK] Orchestration Event Processing failed for event type {}", event.getEventType(), ex);
      return true;
    }
  }

  public boolean isProcessable(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.get(SERVICE_NAME) != null) {
        if (serviceName.equals(PmsConstants.INTERNAL_SERVICE_NAME)) {
          return true;
        }
        return serviceName.equals(metadataMap.get(SERVICE_NAME));
      }
    }
    return false;
  }
}
