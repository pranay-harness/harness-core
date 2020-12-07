package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_END;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_START;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.PLAN_EXECUTION_STATUS_UPDATE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.NodeExecutionStatusUpdateEventHandlerV2;
import io.harness.event.OrchestrationEndEventHandler;
import io.harness.event.OrchestrationStartEventHandler;
import io.harness.event.PlanExecutionStatusUpdateEventHandler;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.registries.registrar.OrchestrationEventHandlerRegistrar;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public class OrchestrationVisualizationModuleEventHandlerRegistrar implements OrchestrationEventHandlerRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<OrchestrationEventType, OrchestrationEventHandler>> handlerClasses) {
    handlerClasses.add(
        Pair.of(NODE_EXECUTION_STATUS_UPDATE, injector.getInstance(NodeExecutionStatusUpdateEventHandlerV2.class)));
    handlerClasses.add(Pair.of(ORCHESTRATION_START, injector.getInstance(OrchestrationStartEventHandler.class)));
    handlerClasses.add(Pair.of(ORCHESTRATION_END, injector.getInstance(OrchestrationEndEventHandler.class)));
    handlerClasses.add(
        Pair.of(PLAN_EXECUTION_STATUS_UPDATE, injector.getInstance(PlanExecutionStatusUpdateEventHandler.class)));
  }
}
