package io.harness.pms.sdk.core.execution.events.orchestration;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.sdk.PmsSdkModuleUtils;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.execution.events.NodeBaseEventHandler;
import io.harness.pms.sdk.core.registries.OrchestrationEventHandlerRegistry;
import io.harness.serializer.ProtoUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class SdkOrchestrationEventHandler extends NodeBaseEventHandler<OrchestrationEvent> {
  @Inject private OrchestrationEventHandlerRegistry handlerRegistry;
  @Inject @Named(PmsSdkModuleUtils.SDK_EXECUTOR_NAME) private ExecutorService executorService;

  @Override
  protected Map<String, String> extraLogProperties(OrchestrationEvent event) {
    return ImmutableMap.<String, String>builder().put("eventType", event.getEventType().name()).build();
  }

  @Override
  protected Ambiance extractAmbiance(OrchestrationEvent event) {
    return event.getAmbiance();
  }

  @Override
  protected boolean handleEventWithContext(OrchestrationEvent event) {
    Set<OrchestrationEventHandler> handlers = handlerRegistry.obtain(event.getEventType());
    if (isNotEmpty(handlers)) {
      // Todo: Add exception handling here.
      handlers.forEach(handler -> executorService.submit(() -> handler.handleEvent(buildSdkOrchestrationEvent(event))));
    }
    return true;
  }

  private io.harness.pms.sdk.core.events.OrchestrationEvent buildSdkOrchestrationEvent(OrchestrationEvent event) {
    return io.harness.pms.sdk.core.events.OrchestrationEvent.builder()
        .eventType(event.getEventType())
        .ambiance(event.getAmbiance())
        .createdAt(ProtoUtils.timestampToUnixMillis(event.getCreatedAt()))
        .status(event.getStatus())
        .resolvedStepParameters(event.getStepParameters().toStringUtf8())
        .serviceName(event.getServiceName())
        .triggerPayload(event.getTriggerPayload())
        .build();
  }
}
