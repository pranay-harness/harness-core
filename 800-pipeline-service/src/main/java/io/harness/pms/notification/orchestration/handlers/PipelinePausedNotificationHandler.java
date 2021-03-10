package io.harness.pms.notification.orchestration.handlers;

import io.harness.notification.PipelineEventType;
import io.harness.pms.notification.NotificationHelper;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;

import com.google.inject.Inject;

public class PipelinePausedNotificationHandler implements AsyncOrchestrationEventHandler {
  @Inject NotificationHelper notificationHelper;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    notificationHelper.sendNotification(
        event.getNodeExecutionProto().getAmbiance(), PipelineEventType.PIPELINE_PAUSED, null);
  }
}
