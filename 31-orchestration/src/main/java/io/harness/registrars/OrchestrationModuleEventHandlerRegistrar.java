package io.harness.registrars;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.OrchestrationStartEventHandler;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.registries.registrar.OrchestrationEventHandlerRegistrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class OrchestrationModuleEventHandlerRegistrar implements OrchestrationEventHandlerRegistrar {
  @Override
  public void register(Set<Pair<OrchestrationEventType, Class<? extends OrchestrationEventHandler>>> handlerClasses) {
    handlerClasses.add(Pair.of(OrchestrationEventType.ORCHESTRATION_START, OrchestrationStartEventHandler.class));
  }
}
