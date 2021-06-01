package io.harness.pms.plan.execution.registrar;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_START;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.PLAN_EXECUTION_STATUS_UPDATE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.plan.execution.handlers.ExecutionInfoUpdateEventHandler;
import io.harness.pms.plan.execution.handlers.ExecutionSummaryCreateEventHandler;
import io.harness.pms.plan.execution.handlers.PipelineStatusUpdateEventHandler;
import io.harness.pms.plan.execution.handlers.PlanStatusEventEmitterHandler;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.registrars.OrchestrationModuleRegistrarHelper;
import io.harness.registrars.OrchestrationVisualizationModuleEventHandlerRegistrar;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class PmsOrchestrationEventRegistrar {
  public Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> getEngineEventHandlers() {
    Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> engineEventHandlersMap =
        new HashMap<>();
    engineEventHandlersMap.put(ORCHESTRATION_START, Sets.newHashSet(ExecutionSummaryCreateEventHandler.class));
    engineEventHandlersMap.put(PLAN_EXECUTION_STATUS_UPDATE,
        Sets.newHashSet(ExecutionInfoUpdateEventHandler.class, PlanStatusEventEmitterHandler.class,
            PipelineStatusUpdateEventHandler.class));
    OrchestrationModuleRegistrarHelper.mergeEventHandlers(
        engineEventHandlersMap, OrchestrationVisualizationModuleEventHandlerRegistrar.getEngineEventHandlers());
    return engineEventHandlersMap;
  }
}
