package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.OrchestrationGraphGenerationHandler;
import io.harness.event.OrchestrationStartEventHandler;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class OrchestrationVisualizationModuleEventHandlerRegistrar {
  public Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> getEngineEventHandlers() {
    Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> engineEventHandlersMap =
        new HashMap<>();
    engineEventHandlersMap.put(
        OrchestrationEventType.ORCHESTRATION_START, Sets.newHashSet(OrchestrationStartEventHandler.class));
    engineEventHandlersMap.put(
        OrchestrationEventType.ORCHESTRATION_END, Sets.newHashSet(OrchestrationGraphGenerationHandler.class));
    engineEventHandlersMap.put(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE,
        Sets.newHashSet(OrchestrationGraphGenerationHandler.class));
    engineEventHandlersMap.put(
        OrchestrationEventType.NODE_EXECUTION_UPDATE, Sets.newHashSet(OrchestrationGraphGenerationHandler.class));
    engineEventHandlersMap.put(OrchestrationEventType.PLAN_EXECUTION_STATUS_UPDATE,
        Sets.newHashSet(OrchestrationGraphGenerationHandler.class));
    return engineEventHandlersMap;
  }
}
