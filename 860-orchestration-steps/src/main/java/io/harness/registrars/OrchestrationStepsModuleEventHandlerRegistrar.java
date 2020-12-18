package io.harness.registrars;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.steps.barriers.BarrierInitializer;

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class OrchestrationStepsModuleEventHandlerRegistrar {
  public Map<OrchestrationEventType, OrchestrationEventHandler> getEngineEventHandlers(Injector injector) {
    Map<OrchestrationEventType, OrchestrationEventHandler> engineEventHandlersMap = new HashMap<>();
    engineEventHandlersMap.put(
        OrchestrationEventType.ORCHESTRATION_START, injector.getInstance(BarrierInitializer.class));
    engineEventHandlersMap.putAll(OrchestrationModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    return engineEventHandlersMap;
  }
}
