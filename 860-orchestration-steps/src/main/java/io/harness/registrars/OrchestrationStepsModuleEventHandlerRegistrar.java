package io.harness.registrars;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.steps.barriers.BarrierInitializer;

import com.google.common.collect.Sets;
import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class OrchestrationStepsModuleEventHandlerRegistrar {
  public Map<OrchestrationEventType, Set<OrchestrationEventHandler>> getEngineEventHandlers(Injector injector) {
    Map<OrchestrationEventType, Set<OrchestrationEventHandler>> engineEventHandlersMap = new HashMap<>();
    engineEventHandlersMap.put(
        OrchestrationEventType.ORCHESTRATION_START, Sets.newHashSet(injector.getInstance(BarrierInitializer.class)));
    OrchestrationModuleRegistrarHelper.mergeEventHandlers(
        engineEventHandlersMap, OrchestrationModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    return engineEventHandlersMap;
  }
}
