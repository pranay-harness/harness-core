/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.notification.orchestration.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.observers.NodeStatusUpdateObserver;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.engine.utils.OrchestrationUtils;
import io.harness.execution.NodeExecution;
import io.harness.notification.PipelineEventType;
import io.harness.observer.AsyncInformObserver;
import io.harness.plancreator.beans.OrchestrationConstants;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.notification.NotificationHelper;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@OwnedBy(HarnessTeam.PIPELINE)
public class StageStatusUpdateNotificationEventHandler implements AsyncInformObserver, NodeStatusUpdateObserver {
  @Inject @Named("PipelineExecutorService") ExecutorService executorService;
  @Inject NotificationHelper notificationHelper;

  @Override
  public void onNodeStatusUpdate(NodeUpdateInfo nodeUpdateInfo) {
    NodeExecution nodeExecution = nodeUpdateInfo.getNodeExecution();
    if (OrchestrationUtils.isStageNode(nodeExecution)) {
      Optional<PipelineEventType> pipelineEventType = notificationHelper.getEventTypeForStage(nodeExecution);
      pipelineEventType.ifPresent(eventType
          -> notificationHelper.sendNotification(
              nodeExecution.getAmbiance(), eventType, nodeExecution, nodeUpdateInfo.getUpdatedTs()));
      return;
    }

    Level currentLevel = Objects.requireNonNull(AmbianceUtils.obtainCurrentLevel(nodeExecution.getAmbiance()));
    String group = currentLevel.getGroup();
    String identifier = currentLevel.getIdentifier();
    if (Objects.equals(group, StepOutcomeGroup.STAGES.name()) || Objects.equals(group, StepOutcomeGroup.PIPELINE.name())
        || Objects.equals(group, StepOutcomeGroup.EXECUTION.name())
        || Objects.equals(group, StepOutcomeGroup.STEP_GROUP.name())
        || identifier.endsWith(OrchestrationConstants.ROLLBACK_NODE_NAME)) {
      return;
    }
    if (!Objects.equals(nodeExecution.getNode().getSkipGraphType(), SkipType.SKIP_NODE)
        && StatusUtils.brokeStatuses().contains(nodeExecution.getStatus())) {
      notificationHelper.sendNotification(
          nodeExecution.getAmbiance(), PipelineEventType.STEP_FAILED, nodeExecution, nodeUpdateInfo.getUpdatedTs());
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
