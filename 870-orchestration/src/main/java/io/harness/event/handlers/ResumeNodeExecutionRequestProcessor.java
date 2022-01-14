package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.pms.contracts.execution.events.ResumeNodeExecutionRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class ResumeNodeExecutionRequestProcessor implements SdkResponseProcessor {
  @Inject OrchestrationEngine engine;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    ResumeNodeExecutionRequest request = event.getResumeNodeExecutionRequest();
    engine.resumeNodeExecution(event.getAmbiance(), request.getResponseMap(), request.getAsyncError());
  }
}
