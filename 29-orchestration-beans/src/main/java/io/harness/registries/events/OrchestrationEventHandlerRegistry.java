package io.harness.registries.events;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.execution.events.OrchestrationSubject;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OwnedBy(HarnessTeam.CDC)
public class OrchestrationEventHandlerRegistry
    implements Registry<OrchestrationEventType, Class<? extends OrchestrationEventHandler>> {
  @Inject Injector injector;
  private Map<OrchestrationEventType, OrchestrationSubject> registry = new ConcurrentHashMap<>();

  @Override
  public void register(
      OrchestrationEventType registryKey, Class<? extends OrchestrationEventHandler> registrableEntity) {
    OrchestrationSubject subject = registry.computeIfAbsent(registryKey, val -> new OrchestrationSubject(injector));
    subject.register(registrableEntity);
  }

  @Override
  public OrchestrationSubject obtain(OrchestrationEventType orchestrationEventType) {
    return registry.getOrDefault(orchestrationEventType, null);
  }

  @Override
  public String getType() {
    return RegistryType.ORCHESTRATION_EVENT.name();
  }
}
