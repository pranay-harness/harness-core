package io.harness.registrars;

import io.harness.cdng.pipeline.executions.PipelineExecutionStartEventHandler;
import io.harness.cdng.pipeline.executions.PipelineExecutionUpdateEventHandler;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrchestrationExecutionEventHandlerRegistrar {
  public Map<OrchestrationEventType, OrchestrationEventHandler> getEngineEventHandlers(Injector injector) {
    Map<OrchestrationEventType, OrchestrationEventHandler> engineEventHandlersMap = new HashMap<>();
    engineEventHandlersMap.put(
        OrchestrationEventType.ORCHESTRATION_START, injector.getInstance(PipelineExecutionStartEventHandler.class));
    engineEventHandlersMap.put(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE,
        injector.getInstance(PipelineExecutionUpdateEventHandler.class));
    engineEventHandlersMap.putAll(
        OrchestrationVisualizationModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    engineEventHandlersMap.putAll(OrchestrationStepsModuleEventHandlerRegistrar.getEngineEventHandlers(injector));

    return engineEventHandlersMap;
  }
}
