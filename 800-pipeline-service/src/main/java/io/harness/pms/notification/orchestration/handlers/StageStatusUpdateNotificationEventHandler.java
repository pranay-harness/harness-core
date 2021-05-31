package io.harness.pms.notification.orchestration.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdate;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdateInfo;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.notification.PipelineEventType;
import io.harness.observer.AsyncInformObserver;
import io.harness.plancreator.beans.OrchestrationConstants;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.notification.NotificationHelper;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@OwnedBy(HarnessTeam.PIPELINE)
public class StageStatusUpdateNotificationEventHandler implements AsyncInformObserver, StepStatusUpdate {
  @Inject @Named("PipelineExecutorService") ExecutorService executorService;
  @Inject NotificationHelper notificationHelper;
  @Inject NodeExecutionService nodeExecutionService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    NodeExecution nodeExecution = nodeExecutionService.get(stepStatusUpdateInfo.getNodeExecutionId());
    NodeExecutionProto nodeExecutionProto = NodeExecutionMapper.toNodeExecutionProto(nodeExecution);
    if (Objects.equals(nodeExecution.getNode().getGroup(), StepOutcomeGroup.STAGE.name())) {
      Optional<PipelineEventType> pipelineEventType = notificationHelper.getEventTypeForStage(nodeExecution);
      pipelineEventType.ifPresent(
          eventType -> notificationHelper.sendNotification(nodeExecution.getAmbiance(), eventType, nodeExecution));
      return;
    }
    if (Objects.equals(nodeExecution.getNode().getGroup(), StepOutcomeGroup.STAGES.name())
        || Objects.equals(nodeExecution.getNode().getGroup(), StepOutcomeGroup.PIPELINE.name())
        || Objects.equals(nodeExecution.getNode().getGroup(), StepOutcomeGroup.EXECUTION.name())
        || Objects.equals(nodeExecution.getNode().getGroup(), StepOutcomeGroup.STEP_GROUP.name())
        || nodeExecution.getNode().getIdentifier().endsWith(OrchestrationConstants.ROLLBACK_NODE_NAME)) {
      return;
    }
    if (!Objects.equals(nodeExecution.getNode().getSkipType(), SkipType.SKIP_NODE)
        && StatusUtils.brokeStatuses().contains(nodeExecutionProto.getStatus())) {
      notificationHelper.sendNotification(nodeExecution.getAmbiance(), PipelineEventType.STEP_FAILED, nodeExecution);
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
