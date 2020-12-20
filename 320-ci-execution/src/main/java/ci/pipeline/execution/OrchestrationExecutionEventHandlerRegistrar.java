package ci.pipeline.execution;

import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.registrars.OrchestrationModuleEventHandlerRegistrar;
import io.harness.registrars.OrchestrationModuleRegistrarHelper;
import io.harness.registrars.OrchestrationStepsModuleEventHandlerRegistrar;
import io.harness.registrars.OrchestrationVisualizationModuleEventHandlerRegistrar;

import com.google.common.collect.Sets;
import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrchestrationExecutionEventHandlerRegistrar {
  public Map<OrchestrationEventType, Set<OrchestrationEventHandler>> getEngineEventHandlers(Injector injector) {
    Map<OrchestrationEventType, Set<OrchestrationEventHandler>> engineEventHandlersMap = new HashMap<>();
    engineEventHandlersMap.put(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE,
        Sets.newHashSet(injector.getInstance(PipelineExecutionUpdateEventHandler.class)));
    OrchestrationModuleRegistrarHelper.mergeEventHandlers(
        engineEventHandlersMap, OrchestrationVisualizationModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    OrchestrationModuleRegistrarHelper.mergeEventHandlers(
        engineEventHandlersMap, OrchestrationStepsModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    engineEventHandlersMap.putAll(OrchestrationModuleEventHandlerRegistrar.getEngineEventHandlers(injector));
    return engineEventHandlersMap;
  }
}
