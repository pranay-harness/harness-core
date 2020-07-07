package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.barriers.BarrierExecutionInstance;
import io.harness.beans.EmbeddedUser;
import io.harness.distribution.barrier.Barrier.State;
import io.harness.engine.barriers.BarrierService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.state.core.barrier.BarrierStep;
import io.harness.state.core.barrier.BarrierStepParameters;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;

@Redesign
@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class OrchestrationServiceImpl implements OrchestrationService {
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private BarrierService barrierService;

  @Override
  public PlanExecution startExecution(Plan plan, EmbeddedUser createdBy) {
    return startExecution(plan, null, createdBy);
  }

  public PlanExecution startExecution(@Valid Plan plan, Map<String, String> setupAbstractions, EmbeddedUser createdBy) {
    PlanExecution planExecution = PlanExecution.builder()
                                      .uuid(generateUuid())
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
    constructBarriers(savedPlanExecution);
    Ambiance ambiance =
        Ambiance.builder().setupAbstractions(setupAbstractions).planExecutionId(savedPlanExecution.getUuid()).build();
    orchestrationEngine.triggerExecution(ambiance, planNode);
    return savedPlanExecution;
  }

  private void constructBarriers(PlanExecution planExecution) {
    barrierService.saveAll(
        planExecution.getPlan()
            .getNodes()
            .stream()
            .filter(planNode -> planNode.getStepType() == BarrierStep.STEP_TYPE)
            .map(planNode
                -> BarrierExecutionInstance.builder()
                       .uuid(generateUuid())
                       .name(planNode.getName())
                       .planNodeId(planNode.getUuid())
                       .identifier(((BarrierStepParameters) planNode.getStepParameters()).getIdentifier())
                       .planExecutionId(planExecution.getUuid())
                       .barrierState(State.STANDING)
                       .expiredIn(((BarrierStepParameters) planNode.getStepParameters()).getTimeoutInMillis())
                       .build())
            .collect(Collectors.toList()));
  }
}
