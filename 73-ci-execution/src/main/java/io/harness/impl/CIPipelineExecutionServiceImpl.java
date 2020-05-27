package io.harness.impl;

import com.google.inject.Inject;

import io.harness.beans.CIPipeline;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.stages.IntegrationStage;
import io.harness.engine.ExecutionEngine;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.BasicExecutionPlanGenerator;
import io.harness.plan.input.InputArgs;

public class CIPipelineExecutionServiceImpl implements CIPipelineExecutionService {
  @Inject private ExecutionEngine engine;
  @Inject private BasicExecutionPlanGenerator planGenerator;
  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  private static final String APP_ID = "XEsfW6D_RJm1IaGpDidD3g";
  private static final String K8_CLUSTER_NAME = "kubernetes_clusterqqq";

  public PlanExecution executePipeline(CIPipeline ciPipeline) {
    // TODO iterate all stages properly
    IntegrationStage jobExecutionStage = (IntegrationStage) ciPipeline.getLinkedStages().get(0);

    // TODO set user before execution which will be available once we build authentication
    // User user = UserThreadLocal.get()
    return engine.startExecution(planGenerator.generateExecutionPlan(jobExecutionStage.getStepInfos()),
        InputArgs.builder()
            .put("clusterName", K8_CLUSTER_NAME)
            .put("accountId", ACCOUNT_ID)
            .put("appId", APP_ID)
            .build(),
        EmbeddedUser.builder().uuid("harsh").email("harsh.jain@harness.io").name("harsh jain").build());
  }
}
