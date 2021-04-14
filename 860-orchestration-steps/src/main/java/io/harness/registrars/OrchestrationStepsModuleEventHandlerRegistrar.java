package io.harness.registrars;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.steps.barriers.BarrierInitializer;
import io.harness.steps.barriers.event.BarrierDropper;
import io.harness.steps.barriers.event.BarrierPositionHelperEventHandler;
import io.harness.steps.resourcerestraint.ResourceRestraintInitializer;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class OrchestrationStepsModuleEventHandlerRegistrar {
  public Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> getEngineEventHandlers() {
    Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> engineEventHandlersMap =
        new HashMap<>();
    engineEventHandlersMap.put(OrchestrationEventType.ORCHESTRATION_START,
        Sets.newHashSet(BarrierInitializer.class, ResourceRestraintInitializer.class));
    engineEventHandlersMap.put(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE,
        Sets.newHashSet(BarrierPositionHelperEventHandler.class, BarrierDropper.class));
    OrchestrationModuleRegistrarHelper.mergeEventHandlers(
        engineEventHandlersMap, OrchestrationModuleEventHandlerRegistrar.getEngineEventHandlers());
    return engineEventHandlersMap;
  }
}
