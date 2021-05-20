package io.harness.pms.notification.orchestration.handlers;

import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdate;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdateInfo;
import io.harness.execution.NodeExecution;
import io.harness.notification.PipelineEventType;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.notification.NotificationHelper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;

public class StageStartNotificationHandler implements AsyncInformObserver, StepStatusUpdate {
  @Inject NotificationHelper notificationHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject @Named("PipelineExecutorService") ExecutorService executorService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    NodeExecution nodeExecution = nodeExecutionService.get(stepStatusUpdateInfo.getNodeExecutionId());
    if (notificationHelper.isStageNode(nodeExecution)) {
      notificationHelper.sendNotification(nodeExecution.getAmbiance(), PipelineEventType.STAGE_START, nodeExecution);
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
