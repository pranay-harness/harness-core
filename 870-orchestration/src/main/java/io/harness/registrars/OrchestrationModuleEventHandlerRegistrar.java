package io.harness.registrars;

import static io.harness.pms.contracts.execution.events.OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.NodeExecutionStatusUpdateEventHandler;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class OrchestrationModuleEventHandlerRegistrar {
  public Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> getEngineEventHandlers() {
    return ImmutableMap.<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>>builder()
        .put(NODE_EXECUTION_STATUS_UPDATE, Sets.newHashSet(NodeExecutionStatusUpdateEventHandler.class))
        .build();
  }
}
