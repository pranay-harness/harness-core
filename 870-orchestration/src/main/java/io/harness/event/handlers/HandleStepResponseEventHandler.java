package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.PmsNodeExecutionService;
import io.harness.pms.contracts.execution.events.HandleStepResponseRequest;
import io.harness.pms.execution.SdkResponseEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class HandleStepResponseEventHandler implements SdkResponseEventHandler {
  @Inject private PmsNodeExecutionService nodeExecutionService;

  @Override
  public void handleEvent(SdkResponseEvent event) {
    HandleStepResponseRequest request = event.getSdkResponseEventRequest().getHandleStepResponseRequest();
    nodeExecutionService.handleStepResponse(request.getNodeExecutionId(), request.getStepResponse());
  }
}
