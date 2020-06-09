package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import io.harness.persistence.HPersistence;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.plan.input.InputArgs;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;

@Redesign
@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class EngineServiceImpl implements EngineService {
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject private ExecutionEngine executionEngine;

  public PlanExecution startExecution(@Valid Plan plan, EmbeddedUser createdBy) {
    return startExecution(plan, null, createdBy);
  }

  public PlanExecution startExecution(@Valid Plan plan, InputArgs inputArgs, EmbeddedUser createdBy) {
    PlanExecution planExecution = PlanExecution.builder()
                                      .uuid(generateUuid())
                                      .plan(plan)
                                      .inputArgs(inputArgs == null ? new InputArgs() : inputArgs)
                                      .status(Status.RUNNING)
                                      .createdBy(createdBy)
                                      .startTs(System.currentTimeMillis())
                                      .build();
    PlanNode planNode = plan.fetchStartingNode();
    if (planNode == null) {
      logger.error("Cannot Start Execution for empty plan");
      return null;
    }
    String savedPlanExecutionId = hPersistence.save(planExecution);
    Ambiance ambiance = Ambiance.builder().inputArgs(inputArgs).planExecutionId(savedPlanExecutionId).build();
    executionEngine.triggerExecution(ambiance, planNode);
    return planExecution;
  }
}
