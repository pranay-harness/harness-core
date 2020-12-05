package io.harness.registrars;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.NodeExecutionStatusUpdateEventHandler;
import io.harness.engine.events.OrchestrationEndEventHandler;
import io.harness.engine.events.OrchestrationStartEventHandler;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.registries.registrar.OrchestrationEventHandlerRegistrar;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.CDC)
public class OrchestrationModuleEventHandlerRegistrar implements OrchestrationEventHandlerRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<OrchestrationEventType, OrchestrationEventHandler>> handlerClasses) {
    handlerClasses.add(Pair.of(
        OrchestrationEventType.ORCHESTRATION_START, injector.getInstance(OrchestrationStartEventHandler.class)));
    handlerClasses.add(
        Pair.of(OrchestrationEventType.ORCHESTRATION_END, injector.getInstance(OrchestrationEndEventHandler.class)));
    handlerClasses.add(Pair.of(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE,
        injector.getInstance(NodeExecutionStatusUpdateEventHandler.class)));
  }
}
