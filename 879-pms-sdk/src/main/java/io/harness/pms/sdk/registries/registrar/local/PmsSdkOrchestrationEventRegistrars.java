package io.harness.pms.sdk.registries.registrar.local;

import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.execution.ExecutionSummaryUpdateEventHandler;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PmsSdkOrchestrationEventRegistrars {
  public Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> getHandlers() {
    Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> handlerHashMap = new HashMap<>();
    handlerHashMap.put(
        OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE, Sets.newHashSet(ExecutionSummaryUpdateEventHandler.class));
    return handlerHashMap;
  }
}
