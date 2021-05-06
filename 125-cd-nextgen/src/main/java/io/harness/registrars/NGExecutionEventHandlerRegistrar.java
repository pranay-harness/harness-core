package io.harness.registrars;

import io.harness.cdng.pipeline.executions.PipelineExecutionStartEventHandler;
import io.harness.cdng.pipeline.executions.PipelineExecutionUpdateEventHandler;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGExecutionEventHandlerRegistrar {
  public Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> getEngineEventHandlers(
      boolean remote) {
    Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> engineEventHandlersMap =
        new HashMap<>();
    if (!remote) {
      engineEventHandlersMap.put(
          OrchestrationEventType.ORCHESTRATION_START, Sets.newHashSet(PipelineExecutionStartEventHandler.class));
      engineEventHandlersMap.put(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE,
          Sets.newHashSet(PipelineExecutionUpdateEventHandler.class));
      OrchestrationModuleRegistrarHelper.mergeEventHandlers(
          engineEventHandlersMap, OrchestrationStepsModuleEventHandlerRegistrar.getEngineEventHandlers());
    }
    return engineEventHandlersMap;
  }
}
