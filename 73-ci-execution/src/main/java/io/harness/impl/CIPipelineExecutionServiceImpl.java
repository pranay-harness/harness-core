package io.harness.impl;

import static software.wings.common.CICommonPodConstants.POD_NAME;

import com.google.inject.Inject;

import io.harness.beans.CIPipeline;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.engine.ExecutionEngine;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.BasicExecutionPlanGenerator;
import io.harness.integrationstage.IntegrationStageExecutionModifier;
import io.harness.plan.input.InputArgs;
import io.harness.yaml.core.Execution;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CIPipelineExecutionServiceImpl implements CIPipelineExecutionService {
  @Inject private ExecutionEngine engine;
  @Inject private BasicExecutionPlanGenerator planGenerator;

  private static final String CLUSTER_NAME = "cluster";
  private static final String NAMESPACE = "namespace";
  private static final String PODNAME = "podName";

  public PlanExecution executePipeline(CIPipeline ciPipeline) {
    // TODO iterate all stages properly and all integration stages with run in parallel
    IntegrationStage integrationStage = (IntegrationStage) ciPipeline.getStages().get(0);

    // TODO Check with == once we change type to enum
    if (integrationStage.getType().equals("integration")) {
      String podName = generatePodName(integrationStage);
      IntegrationStageExecutionModifier integrationStageExecutionModifier =
          IntegrationStageExecutionModifier.builder().podName(podName).build();

      Execution execution = integrationStage.getExecution();
      Execution modifiedExecutionPlan =
          integrationStageExecutionModifier.modifyExecutionPlan(execution, integrationStage);

      InputArgs inputArgs =
          InputArgs.builder()
              .put(
                  CLUSTER_NAME, ((K8sDirectInfraYaml) integrationStage.getInfrastructure()).getSpec().getK8sConnector())
              .put(NAMESPACE, ((K8sDirectInfraYaml) integrationStage.getInfrastructure()).getSpec().getNamespace())
              .put(PODNAME, podName)
              .build();

      // TODO set user before execution which will be available once we build authentication
      // User user = UserThreadLocal.get()
      return engine.startExecution(planGenerator.generateExecutionPlan(modifiedExecutionPlan), inputArgs,
          EmbeddedUser.builder().uuid("harsh").email("harsh.jain@harness.io").name("harsh jain").build());
    } else {
      throw new IllegalArgumentException("Only integration stage has been supported");
    }
  }

  private String generatePodName(IntegrationStage integrationStage) {
    // TODO Use better pod naming strategy after discussion with PM
    return POD_NAME + "-" + integrationStage.getIdentifier();
  }
}
