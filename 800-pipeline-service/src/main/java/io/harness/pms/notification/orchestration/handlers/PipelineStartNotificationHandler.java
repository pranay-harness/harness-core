package io.harness.pms.notification.orchestration.handlers;

import io.harness.pms.notification.NotificationHelper;
import io.harness.pms.notification.PipelineEventType;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;

import com.google.inject.Inject;

public class PipelineStartNotificationHandler implements AsyncOrchestrationEventHandler {
  @Inject NotificationHelper notificationHelper;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    notificationHelper.sendNotification(event.getAmbiance(), PipelineEventType.PIPELINE_START, null);
  }
}