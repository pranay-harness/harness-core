package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class OrchestrationEndEventHandler implements AsyncOrchestrationEventHandler {
  @Inject PlanExecutionService planExecutionService;
  @Inject GraphGenerationService graphGenerationService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    NodeExecutionProto nodeExecutionProto = event.getNodeExecutionProto();
    String planExecutionId = nodeExecutionProto.getAmbiance().getPlanExecutionId();
    try {
      PlanExecution planExecution = planExecutionService.get(planExecutionId);
      log.info("Ending Execution for planExecutionId [{}] with status [{}].", planExecution.getUuid(),
          planExecution.getStatus());

      OrchestrationGraph cachedGraph = graphGenerationService.getCachedOrchestrationGraph(planExecution.getUuid());

      graphGenerationService.cacheOrchestrationGraph(
          cachedGraph.withStatus(planExecution.getStatus()).withEndTs(planExecution.getEndTs()));
    } catch (Exception e) {
      log.error("[{}] event failed for [{}] for plan [{}]", event.getEventType(), nodeExecutionProto.getUuid(),
          planExecutionId, e);
    }
  }
}
