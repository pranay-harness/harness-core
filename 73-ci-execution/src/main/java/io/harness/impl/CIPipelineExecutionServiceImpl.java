package io.harness.impl;

import static software.wings.common.CICommonPodConstants.CLUSTER_NAME;
import static software.wings.common.CICommonPodConstants.NAMESPACE;
import static software.wings.common.CICommonPodConstants.PODNAME;
import static software.wings.common.CICommonPodConstants.POD_NAME;

import com.google.inject.Inject;

import io.harness.beans.CIPipeline;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.engine.ExecutionEngine;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.BasicExecutionPlanGenerator;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.plan.Plan;
import io.harness.plan.input.InputArgs;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CIPipelineExecutionServiceImpl implements CIPipelineExecutionService {
  @Inject private ExecutionEngine engine;
  @Inject private BasicExecutionPlanGenerator planGenerator;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;

  public PlanExecution executePipeline(CIPipeline ciPipeline) {
    IntegrationStage integrationStage = (IntegrationStage) ciPipeline.getStages().get(0);
    String podName = generatePodName(integrationStage);
    InputArgs inputArgs =
        InputArgs.builder()
            .put(CLUSTER_NAME, ((K8sDirectInfraYaml) integrationStage.getInfrastructure()).getSpec().getK8sConnector())
            .put(NAMESPACE, ((K8sDirectInfraYaml) integrationStage.getInfrastructure()).getSpec().getNamespace())
            .put(PODNAME, podName)
            .build();

    Plan plan = executionPlanCreatorService.createPlanForPipeline(ciPipeline, ciPipeline.getAccountId());
    // TODO set user before execution which will be available once we build authentication
    // User user = UserThreadLocal.get()
    return engine.startExecution(plan, inputArgs,
        EmbeddedUser.builder().uuid("harsh").email("harsh.jain@harness.io").name("harsh jain").build());
  }

  private String generatePodName(IntegrationStage integrationStage) {
    // TODO Use better pod naming strategy after discussion with PM
    return POD_NAME + "-" + integrationStage.getIdentifier();
  }
}
