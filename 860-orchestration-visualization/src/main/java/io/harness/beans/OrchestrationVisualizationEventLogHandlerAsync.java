package io.harness.beans;

import io.harness.engine.events.OrchestrationEventLogHandler;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.sdk.core.events.OrchestrationEventLog;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class OrchestrationVisualizationEventLogHandlerAsync
    implements OrchestrationEventLogHandler, AsyncInformObserver {
  private static final ExecutorService executor = Executors.newFixedThreadPool(2);
  @Inject GraphGenerationService graphGenerationService;

  @Override
  public void handleLog(OrchestrationEventLog eventLog) {
    graphGenerationService.updateGraph(eventLog.getPlanExecutionId());
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executor;
  }
}
