package ci.pipeline.execution;

import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.registrars.OrchestrationModuleEventHandlerRegistrar;
import io.harness.registrars.OrchestrationStepsModuleEventHandlerRegistrar;
import io.harness.registrars.OrchestrationVisualizationModuleEventHandlerRegistrar;

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrchestrationExecutionEventHandlerRegistrar {
  public Map<OrchestrationEventType, OrchestrationEventHandler> getEngineEventHandlers(Injector injector) {
    Map<OrchestrationEventType, OrchestrationEventHandler> engineEventHandlersMap = new HashMap<>();
    engineEventHandlersMap.put(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE,
        injector.getInstance(PipelineExecutionUpdateEventHandler.class));
    engineEventHandlersMap.putAll(
        OrchestrationVisualizationModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    engineEventHandlersMap.putAll(OrchestrationStepsModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    engineEventHandlersMap.putAll(OrchestrationModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    return engineEventHandlersMap;
  }
}
