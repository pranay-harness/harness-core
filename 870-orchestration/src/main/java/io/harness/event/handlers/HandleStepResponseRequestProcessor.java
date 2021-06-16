package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.pms.contracts.execution.events.HandleStepResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class HandleStepResponseRequestProcessor implements SdkResponseProcessor {
  @Inject private OrchestrationEngine engine;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    HandleStepResponseRequest request = event.getSdkResponseEventRequest().getHandleStepResponseRequest();
    engine.handleStepResponse(request.getNodeExecutionId(), request.getStepResponse());
  }
}
