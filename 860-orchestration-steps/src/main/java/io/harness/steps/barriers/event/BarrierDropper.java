/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.barriers.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.observers.NodeStatusUpdateObserver;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.execution.NodeExecution;
import io.harness.observer.AsyncInformObserver;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.steps.barriers.BarrierSpecParameters;
import io.harness.steps.barriers.BarrierStep;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.service.BarrierService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class BarrierDropper implements AsyncInformObserver, NodeStatusUpdateObserver {
  @Inject @Named("OrchestrationVisualizationExecutorService") ExecutorService executorService;
  @Inject private BarrierService barrierService;

  @Override
  public void onNodeStatusUpdate(NodeUpdateInfo nodeUpdateInfo) {
    String planExecutionId = nodeUpdateInfo.getPlanExecutionId();
    try {
      NodeExecution nodeExecution = nodeUpdateInfo.getNodeExecution();
      if (Status.ASYNC_WAITING != nodeExecution.getStatus()
          || !BarrierStep.STEP_TYPE.equals(nodeExecution.getNode().getStepType())) {
        return;
      }
      StepElementParameters stepElementParameters =
          RecastOrchestrationUtils.fromMap(nodeExecution.getResolvedStepParameters(), StepElementParameters.class);
      BarrierSpecParameters barrierSpecParameters = (BarrierSpecParameters) stepElementParameters.getSpec();

      BarrierExecutionInstance barrierExecutionInstance =
          barrierService.findByIdentifierAndPlanExecutionId(barrierSpecParameters.getBarrierRef(), planExecutionId);
      barrierService.update(barrierExecutionInstance);
    } catch (Exception e) {
      log.error("Failed to bring barriers down for planExecutionId: [{}]", planExecutionId);
      throw e;
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
