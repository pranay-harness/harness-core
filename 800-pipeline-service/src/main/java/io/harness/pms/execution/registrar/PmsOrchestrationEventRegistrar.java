package io.harness.pms.execution.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_START;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.handlers.ExecutionSummaryCreateEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.registries.registrar.OrchestrationEventHandlerRegistrar;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public class PmsOrchestrationEventRegistrar implements OrchestrationEventHandlerRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<OrchestrationEventType, OrchestrationEventHandler>> handlerClasses) {
    handlerClasses.add(Pair.of(ORCHESTRATION_START, injector.getInstance(ExecutionSummaryCreateEventHandler.class)));
  }
}
