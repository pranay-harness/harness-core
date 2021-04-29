package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.OrchestrationEndEventHandler;
import io.harness.event.OrchestrationStartEventHandler;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class OrchestrationVisualizationModuleEventHandlerRegistrar {
  public Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> getEngineEventHandlers() {
    Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> engineEventHandlersMap =
        new HashMap<>();
    engineEventHandlersMap.put(
        OrchestrationEventType.ORCHESTRATION_START, Sets.newHashSet(OrchestrationStartEventHandler.class));
    engineEventHandlersMap.put(
        OrchestrationEventType.ORCHESTRATION_END, Sets.newHashSet(OrchestrationEndEventHandler.class));
    return engineEventHandlersMap;
  }
}
