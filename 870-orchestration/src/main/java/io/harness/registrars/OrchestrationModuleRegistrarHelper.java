package io.harness.registrars;

import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;

import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrchestrationModuleRegistrarHelper {
  public void mergeEventHandlers(Map<OrchestrationEventType, Set<OrchestrationEventHandler>> finalHandlers,
      Map<OrchestrationEventType, Set<OrchestrationEventHandler>> handlers) {
    for (Map.Entry<OrchestrationEventType, Set<OrchestrationEventHandler>> entry : handlers.entrySet()) {
      if (finalHandlers.containsKey(entry.getKey())) {
        Set<OrchestrationEventHandler> existing = finalHandlers.get(entry.getKey());
        existing.addAll(entry.getValue());
        finalHandlers.put(entry.getKey(), existing);
      } else {
        finalHandlers.put(entry.getKey(), entry.getValue());
      }
    }
  }
}
