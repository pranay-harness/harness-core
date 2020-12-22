package io.harness.pms.execution.handlers;

import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.SyncOrchestrationEventHandler;

import com.google.inject.Inject;

public class PipelineEntityExecutionDetailsUpdate implements SyncOrchestrationEventHandler {
  @Inject PMSPipelineService pmsPipelineService;

  @Override
  public void handleEvent(OrchestrationEvent event) {}
}
