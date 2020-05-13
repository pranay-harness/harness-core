package io.harness.impl;

import com.google.inject.Inject;

import io.harness.beans.CIPipeline;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.stages.IntegrationStage;
import io.harness.engine.ExecutionEngine;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.BasicExecutionPlanGenerator;

public class CIPipelineExecutionServiceImpl implements CIPipelineExecutionService {
  @Inject private ExecutionEngine engine;
  @Inject private BasicExecutionPlanGenerator planGenerator;

  public PlanExecution executePipeline(CIPipeline ciPipeline) {
    // TODO iterate all stages properly
    IntegrationStage jobExecutionStage = (IntegrationStage) ciPipeline.getLinkedStages().get(0);

    // TODO set user before execution which will be available once we build authentication
    // User user = UserThreadLocal.get()
    return engine.startExecution(planGenerator.generateExecutionPlan(jobExecutionStage.getStepInfos()),
        EmbeddedUser.builder().uuid("harsh").email("harsh.jain@harness.io").name("harsh jain").build());
  }
}
