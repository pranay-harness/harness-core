package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.registries.Registrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public interface OrchestrationEventHandlerRegistrar
    extends Registrar<OrchestrationEventType, OrchestrationEventHandler> {
  void register(Set<Pair<OrchestrationEventType, Class<? extends OrchestrationEventHandler>>> handlerClasses);
}
