package io.harness.pms.sdk.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.registries.Registrar;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public interface OrchestrationEventHandlerRegistrar
    extends Registrar<OrchestrationEventType, OrchestrationEventHandler> {
  void register(Set<Pair<OrchestrationEventType, OrchestrationEventHandler>> handlerClasses);
}
