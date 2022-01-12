/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.MultiServiceOrchestrationWorkflow.MultiServiceOrchestrationWorkflowBuilder;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.concurrency.ConcurrencyStrategy;
import software.wings.yaml.workflow.MultiServiceWorkflowYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/1/17
 */
@OwnedBy(CDC)
@Singleton
public class MultiServiceWorkflowYamlHandler extends WorkflowYamlHandler<MultiServiceWorkflowYaml> {
  @Override
  protected void setOrchestrationWorkflow(WorkflowInfo workflowInfo, WorkflowBuilder workflow) {
    MultiServiceOrchestrationWorkflowBuilder multiServiceWorkflowBuilder =
        MultiServiceOrchestrationWorkflowBuilder.aMultiServiceOrchestrationWorkflow();

    MultiServiceOrchestrationWorkflowBuilder orchestrationWorkflowBuilder =
        multiServiceWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
            .withNotificationRules(workflowInfo.getNotificationRules())
            .withPostDeploymentSteps(workflowInfo.getPostDeploymentSteps())
            .withPreDeploymentSteps(workflowInfo.getPreDeploymentSteps())
            .withRollbackProvisioners(workflowInfo.getRollbackProvisioners())
            .withRollbackWorkflowPhaseIdMap(workflowInfo.getRollbackPhaseMap())
            .withUserVariables(workflowInfo.getUserVariables())
            .withWorkflowPhases(workflowInfo.getPhaseList());
    if (workflowInfo.getConcurrencyStrategy() != null) {
      orchestrationWorkflowBuilder.withConcurrencyStrategy(
          ConcurrencyStrategy.buildFromUnit(workflowInfo.getConcurrencyStrategy()));
    }
    workflow.orchestrationWorkflow(orchestrationWorkflowBuilder.build());
  }

  @Override
  public MultiServiceWorkflowYaml toYaml(Workflow bean, String appId) {
    MultiServiceWorkflowYaml multiServiceWorkflowYaml = MultiServiceWorkflowYaml.builder().build();
    toYaml(multiServiceWorkflowYaml, bean, appId);
    return multiServiceWorkflowYaml;
  }
}
