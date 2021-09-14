/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.plan.execution.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.engine.observers.NodeUpdateObserver;
import io.harness.engine.utils.OrchestrationUtils;
import io.harness.execution.NodeExecution;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;
import io.harness.steps.StepSpecTypeConstants;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class ExecutionSummaryUpdateEventHandler implements NodeUpdateObserver, AsyncInformObserver {
  @Inject private PmsExecutionSummaryRespository pmsExecutionSummaryRepository;
  @Inject @Named("PipelineExecutorService") private ExecutorService executorService;

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }

  @Override
  public void onNodeUpdate(NodeUpdateInfo nodeUpdateInfo) {
    updateStageLevelInfo(nodeUpdateInfo.getPlanExecutionId(), nodeUpdateInfo.getNodeExecution());
    updatePipelineLevelInfo(nodeUpdateInfo.getPlanExecutionId(), nodeUpdateInfo.getNodeExecution());
  }

  public void updatePipelineLevelInfo(String planExecutionId, NodeExecution nodeExecution) {
    if (OrchestrationUtils.isPipelineNode(nodeExecution)) {
      Update update = new Update();
      if (StatusUtils.isFinalStatus(nodeExecution.getStatus())) {
        update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.endTs, nodeExecution.getEndTs());
      }
      Criteria criteria =
          Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
      Query query = new Query(criteria);
      pmsExecutionSummaryRepository.update(query, update);
    }
  }

  public void updateStageLevelInfo(String planExecutionId, NodeExecution nodeExecution) {
    if (OrchestrationUtils.isStageNode(nodeExecution)
        || Objects.equals(nodeExecution.getNode().getStepType().getType(), StepSpecTypeConstants.BARRIER)) {
      Update update = new Update();
      String stageUuid = nodeExecution.getNode().getUuid();
      if (StatusUtils.isFinalStatus(nodeExecution.getStatus())) {
        update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".endTs",
            nodeExecution.getEndTs());
      }
      Criteria criteria =
          Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
      Query query = new Query(criteria);
      pmsExecutionSummaryRepository.update(query, update);
    }
  }
}
