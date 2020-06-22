package io.harness.engine;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import javax.validation.Valid;

@Redesign
@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class EngineServiceImpl implements EngineService {
  @Inject private ExecutionEngine executionEngine;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public PlanExecution startExecution(Plan plan, EmbeddedUser createdBy) {
    return startExecution(plan, null, createdBy);
  }

  public PlanExecution startExecution(@Valid Plan plan, Map<String, String> setupAbstractions, EmbeddedUser createdBy) {
    PlanExecution planExecution = PlanExecution.builder()
                                      .plan(plan)
                                      .setupAbstractions(setupAbstractions)
                                      .status(Status.RUNNING)
                                      .createdBy(createdBy)
                                      .startTs(System.currentTimeMillis())
                                      .build();
    PlanNode planNode = plan.fetchStartingNode();
    if (planNode == null) {
      logger.error("Cannot Start Execution for empty plan");
      return null;
    }
    PlanExecution savedPlanExecution = planExecutionService.save(planExecution);
    Ambiance ambiance =
        Ambiance.builder().setupAbstractions(setupAbstractions).planExecutionId(savedPlanExecution.getUuid()).build();
    executionEngine.triggerExecution(ambiance, planNode);
    return savedPlanExecution;
  }
}
