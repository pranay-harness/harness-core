package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class OrchestrationEndEventHandler {
  @Inject PlanExecutionService planExecutionService;
  @Inject GraphGenerationService graphGenerationService;

  public OrchestrationGraph handleEvent(OrchestrationEvent event, OrchestrationGraph orchestrationGraph) {
    NodeExecutionProto nodeExecutionProto = event.getNodeExecutionProto();
    String planExecutionId = nodeExecutionProto.getAmbiance().getPlanExecutionId();
    try {
      PlanExecution planExecution = planExecutionService.get(planExecutionId);
      log.info("Ending Execution for planExecutionId [{}] with status [{}].", planExecution.getUuid(),
          planExecution.getStatus());

      return orchestrationGraph.withStatus(planExecution.getStatus()).withEndTs(planExecution.getEndTs());
    } catch (Exception e) {
      log.error("[{}] event failed for [{}] for plan [{}]", event.getEventType(), nodeExecutionProto.getUuid(),
          planExecutionId, e);
      throw e;
    }
  }
}
