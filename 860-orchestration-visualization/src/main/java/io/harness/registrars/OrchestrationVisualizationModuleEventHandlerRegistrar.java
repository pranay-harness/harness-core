package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.NODE_EXECUTION_UPDATE;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_END;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_START;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.PLAN_EXECUTION_STATUS_UPDATE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.NodeExecutionStatusUpdateEventHandlerV2;
import io.harness.event.NodeExecutionUpdateEventHandler;
import io.harness.event.OrchestrationEndEventHandler;
import io.harness.event.OrchestrationStartEventHandler;
import io.harness.event.PlanExecutionStatusUpdateEventHandler;
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
        NODE_EXECUTION_STATUS_UPDATE, Sets.newHashSet(NodeExecutionStatusUpdateEventHandlerV2.class));
    engineEventHandlersMap.put(NODE_EXECUTION_UPDATE, Sets.newHashSet(NodeExecutionUpdateEventHandler.class));
    engineEventHandlersMap.put(ORCHESTRATION_START, Sets.newHashSet(OrchestrationStartEventHandler.class));
    engineEventHandlersMap.put(ORCHESTRATION_END, Sets.newHashSet(OrchestrationEndEventHandler.class));
    engineEventHandlersMap.put(
        PLAN_EXECUTION_STATUS_UPDATE, Sets.newHashSet(PlanExecutionStatusUpdateEventHandler.class));
    return engineEventHandlersMap;
  }
}
