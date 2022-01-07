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

import software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowPhase;
import software.wings.yaml.workflow.BuildWorkflowYaml;

import com.google.inject.Singleton;
import java.util.List;
/**
 * @author rktummala on 11/1/17
 */
@OwnedBy(CDC)
@Singleton
public class BuildWorkflowYamlHandler extends WorkflowYamlHandler<BuildWorkflowYaml> {
  @Override
  protected void setOrchestrationWorkflow(WorkflowInfo workflowInfo, WorkflowBuilder workflow) {
    BuildOrchestrationWorkflowBuilder buildWorkflowBuilder =
        BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow();

    List<WorkflowPhase> phaseList = workflowInfo.getPhaseList();
    if (isNotEmpty(phaseList)) {
      WorkflowPhase workflowPhase = phaseList.get(0);
      workflow.infraMappingId(workflowPhase.getInfraMappingId()).serviceId(workflowPhase.getServiceId());
    }

    buildWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
        .withNotificationRules(workflowInfo.getNotificationRules())
        .withPostDeploymentSteps(workflowInfo.getPostDeploymentSteps())
        .withPreDeploymentSteps(workflowInfo.getPreDeploymentSteps())
        .withRollbackProvisioners(workflowInfo.getRollbackProvisioners())
        .withRollbackWorkflowPhaseIdMap(workflowInfo.getRollbackPhaseMap())
        .withUserVariables(workflowInfo.getUserVariables())
        .withWorkflowPhases(phaseList);
    workflow.orchestrationWorkflow(buildWorkflowBuilder.build());
  }

  @Override
  public BuildWorkflowYaml toYaml(Workflow bean, String appId) {
    BuildWorkflowYaml buildWorkflowYaml = BuildWorkflowYaml.builder().build();
    toYaml(buildWorkflowYaml, bean, appId);
    return buildWorkflowYaml;
  }
}
