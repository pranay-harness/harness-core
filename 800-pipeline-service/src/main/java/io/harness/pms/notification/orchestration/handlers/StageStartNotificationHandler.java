package io.harness.pms.notification.orchestration.handlers;

import io.harness.notification.PipelineEventType;
import io.harness.pms.notification.NotificationHelper;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;

import com.google.inject.Inject;

public class StageStartNotificationHandler implements AsyncOrchestrationEventHandler {
  @Inject NotificationHelper notificationHelper;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    if (notificationHelper.isStageNode(event.getNodeExecutionProto())) {
      notificationHelper.sendNotification(
          event.getNodeExecutionProto().getAmbiance(), PipelineEventType.STAGE_START, event.getNodeExecutionProto());
    }
  }
}
