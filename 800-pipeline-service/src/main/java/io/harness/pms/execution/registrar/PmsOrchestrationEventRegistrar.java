package io.harness.pms.execution.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_START;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.handlers.ExecutionSummaryCreateEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.registrars.OrchestrationModuleEventHandlerRegistrar;
import io.harness.registrars.OrchestrationModuleRegistrarHelper;
import io.harness.registrars.OrchestrationVisualizationModuleEventHandlerRegistrar;

import com.google.common.collect.Sets;
import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class PmsOrchestrationEventRegistrar {
  public Map<OrchestrationEventType, Set<OrchestrationEventHandler>> getEngineEventHandlers(Injector injector) {
    Map<OrchestrationEventType, Set<OrchestrationEventHandler>> engineEventHandlersMap = new HashMap<>();
    engineEventHandlersMap.put(
        ORCHESTRATION_START, Sets.newHashSet(injector.getInstance(ExecutionSummaryCreateEventHandler.class)));
    OrchestrationModuleRegistrarHelper.mergeEventHandlers(
        engineEventHandlersMap, OrchestrationVisualizationModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    OrchestrationModuleRegistrarHelper.mergeEventHandlers(
        engineEventHandlersMap, OrchestrationModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    return engineEventHandlersMap;
  }
}
