package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.*;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.NodeExecutionStatusUpdateEventHandlerV2;
import io.harness.event.OrchestrationEndEventHandler;
import io.harness.event.OrchestrationStartEventHandler;
import io.harness.event.PlanExecutionStatusUpdateEventHandler;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;

import com.google.common.collect.Sets;
import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class OrchestrationVisualizationModuleEventHandlerRegistrar {
  public Map<OrchestrationEventType, Set<OrchestrationEventHandler>> getEngineEventHandlers(Injector injector) {
    Map<OrchestrationEventType, Set<OrchestrationEventHandler>> engineEventHandlersMap = new HashMap<>();
    engineEventHandlersMap.put(NODE_EXECUTION_STATUS_UPDATE,
        Sets.newHashSet(injector.getInstance(NodeExecutionStatusUpdateEventHandlerV2.class)));
    engineEventHandlersMap.put(
        ORCHESTRATION_START, Sets.newHashSet(injector.getInstance(OrchestrationStartEventHandler.class)));
    engineEventHandlersMap.put(
        ORCHESTRATION_END, Sets.newHashSet(injector.getInstance(OrchestrationEndEventHandler.class)));
    engineEventHandlersMap.put(PLAN_EXECUTION_STATUS_UPDATE,
        Sets.newHashSet(injector.getInstance(PlanExecutionStatusUpdateEventHandler.class)));

    return engineEventHandlersMap;
  }
}
