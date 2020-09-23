package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.NodeExecutionStatusUpdateEventHandlerV2;
import io.harness.event.OrchestrationEndEventHandler;
import io.harness.event.OrchestrationStartEventHandler;
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
        Pair.of(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE, NodeExecutionStatusUpdateEventHandlerV2.class));
    handlerClasses.add(Pair.of(OrchestrationEventType.ORCHESTRATION_START, OrchestrationStartEventHandler.class));
    handlerClasses.add(Pair.of(OrchestrationEventType.ORCHESTRATION_END, OrchestrationEndEventHandler.class));
  }
}
