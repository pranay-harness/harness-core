/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.concurrency.ConcurrencyStrategy;
import software.wings.yaml.workflow.CanaryWorkflowYaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/1/17
 */
@OwnedBy(CDC)
@Singleton
@TargetModule(HarnessModule._955_CG_YAML)
public class CanaryWorkflowYamlHandler extends WorkflowYamlHandler<CanaryWorkflowYaml> {
  @Override
  protected void setOrchestrationWorkflow(WorkflowInfo workflowInfo, WorkflowBuilder workflow) {
    CanaryOrchestrationWorkflowBuilder canaryOrchestrationWorkflowBuilder =
        CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow();

    CanaryOrchestrationWorkflowBuilder orchestrationWorkflowBuilder =
        canaryOrchestrationWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
            .withNotificationRules(workflowInfo.getNotificationRules())
            .withPostDeploymentSteps(workflowInfo.getPostDeploymentSteps())
            .withPreDeploymentSteps(workflowInfo.getPreDeploymentSteps())
            .withRollbackProvisioners(workflowInfo.getRollbackProvisioners())
            .withRollbackProvisionersReverse(workflowInfo.getRollbackProvisionersReverse())
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
  public CanaryWorkflowYaml toYaml(Workflow bean, String appId) {
    CanaryWorkflowYaml canaryWorkflowYaml = CanaryWorkflowYaml.builder().build();
    toYaml(canaryWorkflowYaml, bean, appId);
    return canaryWorkflowYaml;
  }
}
