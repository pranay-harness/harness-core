/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.RollingOrchestrationWorkflow.RollingOrchestrationWorkflowBuilder;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.concurrency.ConcurrencyStrategy;
import software.wings.yaml.workflow.RollingWorkflowYaml;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/1/17
 */
@OwnedBy(CDC)
@Singleton
public class RollingWorkflowYamlHandler extends WorkflowYamlHandler<RollingWorkflowYaml> {
  @Override
  protected void setOrchestrationWorkflow(WorkflowInfo workflowInfo, WorkflowBuilder workflow) {
    RollingOrchestrationWorkflowBuilder rollingOrchestrationWorkflowBuilder =
        RollingOrchestrationWorkflowBuilder.aRollingOrchestrationWorkflow();

    List<WorkflowPhase> phaseList = workflowInfo.getPhaseList();
    if (isNotEmpty(phaseList)) {
      WorkflowPhase workflowPhase = phaseList.get(0);
      workflow.infraMappingId(workflowPhase.getInfraMappingId()).serviceId(workflowPhase.getServiceId());
      workflow.infraDefinitionId(workflowPhase.getInfraDefinitionId());
    }

    rollingOrchestrationWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
        .withNotificationRules(workflowInfo.getNotificationRules())
        .withPostDeploymentSteps(workflowInfo.getPostDeploymentSteps())
        .withPreDeploymentSteps(workflowInfo.getPreDeploymentSteps())
        .withRollbackProvisioners(workflowInfo.getRollbackProvisioners())
        .withRollbackWorkflowPhaseIdMap(workflowInfo.getRollbackPhaseMap())
        .withUserVariables(workflowInfo.getUserVariables())
        .withWorkflowPhases(phaseList);
    if (workflowInfo.getConcurrencyStrategy() != null) {
      rollingOrchestrationWorkflowBuilder.withConcurrencyStrategy(
          ConcurrencyStrategy.buildFromUnit(workflowInfo.getConcurrencyStrategy()));
    }
    workflow.orchestrationWorkflow(rollingOrchestrationWorkflowBuilder.build());
  }

  @Override
  public RollingWorkflowYaml toYaml(Workflow bean, String appId) {
    RollingWorkflowYaml rollingWorkflowYaml = RollingWorkflowYaml.builder().build();
    toYaml(rollingWorkflowYaml, bean, appId);
    return rollingWorkflowYaml;
  }
}
