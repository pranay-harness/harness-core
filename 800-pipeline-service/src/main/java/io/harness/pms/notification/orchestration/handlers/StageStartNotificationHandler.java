package io.harness.pms.notification.orchestration.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.observers.NodeExecutionStartObserver;
import io.harness.execution.NodeExecution;
import io.harness.notification.PipelineEventType;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.notification.NotificationHelper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;

@OwnedBy(HarnessTeam.PIPELINE)
public class StageStartNotificationHandler implements AsyncInformObserver, NodeExecutionStartObserver {
  @Inject @Named("PipelineExecutorService") ExecutorService executorService;
  @Inject NotificationHelper notificationHelper;

  @Override
  public void onNodeStart(OrchestrationEventType eventType, NodeExecution nodeExecution) {
    if (notificationHelper.isStageNode(nodeExecution)) {
      notificationHelper.sendNotification(nodeExecution.getAmbiance(), PipelineEventType.STAGE_START, nodeExecution);
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
