package io.harness.ng;

import static io.harness.pms.contracts.execution.events.OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE;

import com.google.common.collect.Sets;

import io.harness.PipelineExecutionUpdateEventHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
public class OrchestrationExecutionEventHandlerRegistrar {
  public Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> getEngineEventHandlers() {
    Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> handlerMap = new HashMap<>();
    handlerMap.put(NODE_EXECUTION_STATUS_UPDATE, Sets.newHashSet(PipelineExecutionUpdateEventHandler.class));
    return handlerMap;
  }
}
