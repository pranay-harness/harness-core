package io.harness.event.handlers;

import io.harness.engine.executions.node.PmsNodeExecutionServiceImpl;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.execution.SdkResponseEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AddExecutableResponseEventHandler implements SdkResponseEventHandler {
  @Inject private PmsNodeExecutionServiceImpl nodeExecutionService;

  @Override
  public void handleEvent(SdkResponseEvent event) {
    AddExecutableResponseRequest request = event.getSdkResponseEventRequest().getAddExecutableResponseRequest();
    nodeExecutionService.addExecutableResponse(request.getNodeExecutionId(), request.getStatus(),
        request.getExecutableResponse(), request.getCallbackIdsList());
  }
}
