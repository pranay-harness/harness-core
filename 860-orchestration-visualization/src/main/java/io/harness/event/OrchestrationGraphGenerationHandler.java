package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class OrchestrationGraphGenerationHandler implements AsyncOrchestrationEventHandler {
  @Inject private GraphGenerationService graphGenerationService;
  private static final long THRESHOLD = 1000L;
  @Override
  public void handleEvent(OrchestrationEvent event) {
    try {
      long startTime = System.currentTimeMillis();
      if (event.getNodeExecutionProto() == null) {
        graphGenerationService.buildOrchestrationGraphBasedOnLogs(event.getAmbiance().getPlanExecutionId());
      } else {
        graphGenerationService.buildOrchestrationGraphBasedOnLogs(
            event.getNodeExecutionProto().getAmbiance().getPlanExecutionId());
      }
      long processingTime = System.currentTimeMillis() - startTime;
      if (processingTime > THRESHOLD) {
        log.warn("Time Taken to update graph with planExecutionId [{}] is [{}]ms ",
            event.getAmbiance().getPlanExecutionId(), processingTime);
      } else {
        log.info("Time Taken to update graph with planExecutionId [{}] is [{}]ms ",
            event.getAmbiance().getPlanExecutionId(), processingTime);
      }
    } catch (Exception e) {
      log.error("[{}] event failed for [{}] for plan [{}]", event.getEventType(),
          AmbianceUtils.obtainCurrentRuntimeId(event.getAmbiance()), event.getAmbiance().getPlanExecutionId(), e);
    }
  }
}
