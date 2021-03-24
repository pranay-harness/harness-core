package io.harness.event.handlers;

import io.harness.engine.executions.node.PmsNodeExecutionServiceImpl;
import io.harness.pms.contracts.execution.events.HandleStepResponseRequest;
import io.harness.pms.execution.SdkResponseEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HandleStepResponseEventHandler implements SdkResponseEventHandler {
  @Inject private PmsNodeExecutionServiceImpl nodeExecutionService;

  @Override
  public void handleEvent(SdkResponseEvent event) {
    HandleStepResponseRequest request = event.getSdkResponseEventRequest().getHandleStepResponseRequest();
    nodeExecutionService.handleStepResponse(request.getNodeExecutionId(), request.getStepResponse());
  }
}
