package io.harness.registrars;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.cdng.pipeline.executions.PipelineExecutionStartEventHandler;
import io.harness.cdng.pipeline.executions.PipelineExecutionUpdateEventHandler;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.registries.registrar.OrchestrationEventHandlerRegistrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class OrchestrationExecutionEventHandlerRegistrar implements OrchestrationEventHandlerRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<OrchestrationEventType, OrchestrationEventHandler>> handlerClasses) {
    handlerClasses.add(Pair.of(
        OrchestrationEventType.ORCHESTRATION_START, injector.getInstance(PipelineExecutionStartEventHandler.class)));
    handlerClasses.add(Pair.of(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE,
        injector.getInstance(PipelineExecutionUpdateEventHandler.class)));
  }
}
