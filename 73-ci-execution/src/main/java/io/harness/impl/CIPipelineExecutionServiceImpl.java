package io.harness.impl;

import com.google.inject.Inject;

import io.harness.beans.CIPipeline;
import io.harness.beans.EmbeddedUser;
import io.harness.engine.EngineService;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.BasicExecutionPlanGenerator;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.plan.Plan;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CIPipelineExecutionServiceImpl implements CIPipelineExecutionService {
  @Inject private EngineService engineService;
  @Inject private BasicExecutionPlanGenerator planGenerator;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;

  public PlanExecution executePipeline(CIPipeline ciPipeline) {
    Plan plan = executionPlanCreatorService.createPlanForPipeline(ciPipeline, ciPipeline.getAccountId());
    // TODO set user before execution which will be available once we build authentication
    // User user = UserThreadLocal.get()
    return engineService.startExecution(
        plan, EmbeddedUser.builder().uuid("harsh").email("harsh.jain@harness.io").name("harsh jain").build());
  }
}
