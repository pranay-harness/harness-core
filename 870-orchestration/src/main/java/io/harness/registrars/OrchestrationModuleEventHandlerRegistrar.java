package io.harness.registrars;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.NodeExecutionStatusUpdateEventHandler;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;

import com.google.common.collect.Sets;
import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class OrchestrationModuleEventHandlerRegistrar {
  public Map<OrchestrationEventType, Set<OrchestrationEventHandler>> getEngineEventHandlers(Injector injector) {
    Map<OrchestrationEventType, Set<OrchestrationEventHandler>> engineEventHandlersMap = new HashMap<>();

    engineEventHandlersMap.put(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE,
        Sets.newHashSet(injector.getInstance(NodeExecutionStatusUpdateEventHandler.class)));
    return engineEventHandlersMap;
  }
}
