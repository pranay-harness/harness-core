package io.harness.pms.notification.orchestration.handlers;

import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.notification.NotificationHelper;
import io.harness.pms.notification.PipelineEventType;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.steps.StepOutcomeGroup;

import com.google.inject.Inject;
import java.util.Objects;
import java.util.Optional;

public class StageStatusUpdateNotificationEventHandler implements AsyncOrchestrationEventHandler {
  @Inject NotificationHelper notificationHelper;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    NodeExecutionProto nodeExecutionProto = event.getNodeExecutionProto();
    if (Objects.equals(nodeExecutionProto.getNode().getGroup(), StepOutcomeGroup.STAGE.name())) {
      Optional<PipelineEventType> pipelineEventType = notificationHelper.getEventTypeForStage(nodeExecutionProto);
      pipelineEventType.ifPresent(eventType
          -> notificationHelper.sendNotification(
              event.getNodeExecutionProto().getAmbiance(), eventType, event.getNodeExecutionProto()));
      return;
    }
    if (Objects.equals(nodeExecutionProto.getNode().getGroup(), StepOutcomeGroup.STAGES.name())
        || Objects.equals(nodeExecutionProto.getNode().getGroup(), StepOutcomeGroup.PIPELINE.name())) {
      return;
    }
    if (!Objects.equals(nodeExecutionProto.getNode().getSkipType(), SkipType.SKIP_NODE)
        && StatusUtils.brokeStatuses().contains(nodeExecutionProto.getStatus())) {
      notificationHelper.sendNotification(
          event.getNodeExecutionProto().getAmbiance(), PipelineEventType.STEP_FAILED, event.getNodeExecutionProto());
    }
  }
}
