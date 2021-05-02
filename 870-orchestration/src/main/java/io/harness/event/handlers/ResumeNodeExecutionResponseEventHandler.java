package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.pms.contracts.execution.events.ResumeNodeExecutionRequest;
import io.harness.pms.execution.SdkResponseEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class ResumeNodeExecutionResponseEventHandler implements SdkResponseEventHandler {
  @Inject OrchestrationEngine engine;

  @Override
  public void handleEvent(SdkResponseEvent event) {
    ResumeNodeExecutionRequest request = event.getSdkResponseEventRequest().getResumeNodeExecutionRequest();
    engine.resume(request.getNodeExecutionId(), request.getResponseMap(), request.getAsyncError());
  }
}
