package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.NodeExecutionStatusUpdateEventHandler;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.registries.registrar.OrchestrationEventHandlerRegistrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationVisualizationModuleEventHandlerRegistrar implements OrchestrationEventHandlerRegistrar {
  @Override
  public void register(Set<Pair<OrchestrationEventType, Class<? extends OrchestrationEventHandler>>> handlerClasses) {
    handlerClasses.add(
        Pair.of(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE, NodeExecutionStatusUpdateEventHandler.class));
  }
}
