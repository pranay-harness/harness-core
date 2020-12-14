package io.harness.registrars;

import io.harness.cdng.pipeline.executions.CDExecutionSummaryPmsUpdateEventHandler;
import io.harness.cdng.pipeline.executions.PipelineExecutionStartEventHandler;
import io.harness.cdng.pipeline.executions.PipelineExecutionUpdateEventHandler;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.registries.registrar.OrchestrationEventHandlerRegistrar;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

public class OrchestrationExecutionEventHandlerRegistrar implements OrchestrationEventHandlerRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<OrchestrationEventType, OrchestrationEventHandler>> handlerClasses) {
    handlerClasses.add(Pair.of(
        OrchestrationEventType.ORCHESTRATION_START, injector.getInstance(PipelineExecutionStartEventHandler.class)));
    handlerClasses.add(Pair.of(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE,
        injector.getInstance(PipelineExecutionUpdateEventHandler.class)));
    handlerClasses.add(Pair.of(OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE,
        injector.getInstance(CDExecutionSummaryPmsUpdateEventHandler.class)));
  }
}
