package io.harness.event.handlers;

import io.harness.engine.executions.node.PmsNodeExecutionService;
import io.harness.pms.execution.SdkResponseEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class QueueNodeExecutionEventHandler implements SdkResponseEventHandler {
  @Inject private PmsNodeExecutionService nodeExecutionService;

  @Override
  public void handleEvent(SdkResponseEvent event) {
    nodeExecutionService.queueNodeExecution(
        event.getSdkResponseEventRequest().getQueueNodeExecutionRequest().getNodeExecution());
  }
}
