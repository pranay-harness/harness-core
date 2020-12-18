package io.harness.pms.execution.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_START;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.handlers.ExecutionSummaryCreateEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.registrars.OrchestrationModuleEventHandlerRegistrar;
import io.harness.registrars.OrchestrationVisualizationModuleEventHandlerRegistrar;

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class PmsOrchestrationEventRegistrar {
  public Map<OrchestrationEventType, OrchestrationEventHandler> getEngineEventHandlers(Injector injector) {
    Map<OrchestrationEventType, OrchestrationEventHandler> engineEventHandlersMap = new HashMap<>();
    engineEventHandlersMap.put(ORCHESTRATION_START, injector.getInstance(ExecutionSummaryCreateEventHandler.class));
    engineEventHandlersMap.putAll(
        OrchestrationVisualizationModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    engineEventHandlersMap.putAll(OrchestrationModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    return engineEventHandlersMap;
  }
}
