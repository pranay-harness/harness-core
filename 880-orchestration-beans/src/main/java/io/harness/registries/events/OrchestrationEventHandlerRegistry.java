package io.harness.registries.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@OwnedBy(HarnessTeam.CDC)
public class OrchestrationEventHandlerRegistry
    implements Registry<OrchestrationEventType, Set<OrchestrationEventHandler>> {
  private Map<OrchestrationEventType, Set<OrchestrationEventHandler>> registry = new ConcurrentHashMap<>();

  @Override
  public void register(OrchestrationEventType registryKey, Set<OrchestrationEventHandler> registrableEntity) {
    Set<OrchestrationEventHandler> handlers = registry.computeIfAbsent(registryKey, val -> new HashSet<>());
    handlers.addAll(registrableEntity);
  }

  @Override
  public Set<OrchestrationEventHandler> obtain(OrchestrationEventType orchestrationEventType) {
    return registry.getOrDefault(orchestrationEventType, new HashSet<>());
  }

  @Override
  public String getType() {
    return RegistryType.ORCHESTRATION_EVENT.name();
  }
}
