package io.harness.pms.sdk.execution;

import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.SyncOrchestrationEventHandler;

import com.google.inject.Singleton;

@Singleton
public class ExecutionSummaryCreateEventHandler implements SyncOrchestrationEventHandler {
  @Override
  public void handleEvent(OrchestrationEvent event) {
    // Todo: Add grpc call to create the plan execution summary event.
  }
}
